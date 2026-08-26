package com.liuh886.microuter.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import com.liuh886.microuter.core.model.RecordedClip
import com.liuh886.microuter.core.model.audioTypeLabel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class MicTester(
    private val linkController: (AudioDeviceInfo?) -> Boolean
) {

    val clipReady = MutableSharedFlow<RecordedClip>(extraBufferCapacity = 8)

    private class CaptureState(
        val deviceId: Int,
        val deviceName: String,
        val buffer: ShortArray,
        var fill: Int = 0
    )

    private val captures = LinkedHashMap<Char, CaptureState>()
    private val clipLock = Any()

    @Volatile
    private var lastRate = 0

    fun beginCapture(slot: Char, deviceId: Int, deviceName: String): Boolean {
        if (!running || lastRate <= 0) return false
        val cap = lastRate * RecordedClip.MAX_SECONDS
        synchronized(clipLock) {
            captures[slot] = CaptureState(deviceId, deviceName, ShortArray(cap))
        }
        return true
    }

    fun endCapture(slot: Char) {
        val st = synchronized(clipLock) { captures.remove(slot) } ?: return
        finalizeCapture(slot, st)
    }

    fun clipElapsedMs(slot: Char): Int = synchronized(clipLock) {
        val st = captures[slot] ?: return 0
        if (lastRate <= 0) 0 else (st.fill * 1000 / lastRate)
    }

    private fun finalizeCapture(slot: Char, st: CaptureState) {
        val clip = synchronized(clipLock) {
            if (st.fill == 0) null
            else RecordedClip.finalize(slot, st.deviceId, st.deviceName, st.buffer, st.fill, lastRate)
        } ?: return
        clipReady.tryEmit(clip)
    }

    private fun finalizeAllCaptures() {
        synchronized(clipLock) {
            val iterator = captures.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                iterator.remove()
                finalizeCapture(entry.key, entry.value)
            }
        }
    }

    data class SessionInfo(
        val sampleRate: Int,
        val channelCount: Int,
        val preferredDeviceApplied: Boolean,
        val sourceLabel: String,
        val monitorOutputName: String?,
        val linkConfirmed: Boolean? = null,
        val routedDeviceName: String? = null
    )

    data class Config(
        val device: AudioDeviceInfo?,
        val monitorOutput: AudioDeviceInfo?
    )

    private data class Setup(
        val record: AudioRecord,
        val track: AudioTrack?,
        val info: SessionInfo,
        val isBluetooth: Boolean
    )

    @Volatile
    private var running = false

    @Volatile
    private var pending: Config? = null

    private var worker: Thread? = null
    private val lock = Any()
    private var record: AudioRecord? = null
    private var track: AudioTrack? = null

    val isRunning: Boolean
        get() = running

    fun start(config: Config, onLevel: (Float, SessionInfo) -> Unit, onError: (String) -> Unit) {
        stop()
        running = true
        worker = Thread {
            var cfg = config
            val initial = openSetup(cfg)
            if (initial == null) {
                running = false
                linkController(null)
                onError("Failed to open this input. Try another device.")
                return@Thread
            }
            synchronized(lock) {
                record = initial.record
                track = initial.track
            }
            lastRate = initial.info.sampleRate
            val buffer = ShortArray(initial.record.bufferSizeInFrames.coerceAtLeast(1024))
            var current: Setup = initial
            var currentSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION
            var currentInfo = initial.info
            var silentMs = 0f
            var fallbackDone = false
            while (running) {
                pending?.let { want ->
                    pending = null
                    if (want.device?.id != cfg.device?.id || want.monitorOutput?.id != cfg.monitorOutput?.id) {
                        if (current.isBluetooth && !want.isBluetoothDevice()) linkController(null)
                        finalizeAllCaptures()
                        closeSetup(current)
                        val next = openSetup(want)
                        if (next == null) {
                            cfg = want
                            currentInfo = currentInfo.copy(
                                sourceLabel = "open failed",
                                monitorOutputName = null
                            )
                            onError("Failed to switch to this device.")
                        } else {
                            cfg = want
                            current = next
                            currentInfo = next.info
                            lastRate = next.info.sampleRate
                            currentSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION
                            fallbackDone = false
                            silentMs = 0f
                        }
                    }
                }
                if (!running) break
                val rec = synchronized(lock) { record } ?: break
                val read = rec.read(buffer, 0, buffer.size)
                if (read <= 0) continue
                var sum = 0.0
                var peak = 0
                for (i in 0 until read) {
                    val s = buffer[i].toInt()
                    sum += (s / 32768.0) * (s / 32768.0)
                    if (kotlin.math.abs(s) > peak) peak = kotlin.math.abs(s)
                }
                if (peak < SILENCE_PEAK_THRESHOLD) {
                    silentMs += buffer.size * 1000f / current.info.sampleRate.coerceAtLeast(1)
                    if (!fallbackDone &&
                        !current.isBluetooth &&
                        currentSource == MediaRecorder.AudioSource.VOICE_COMMUNICATION &&
                        silentMs > SILENCE_FALLBACK_MS
                    ) {
                        fallbackDone = true
                        currentSource = MediaRecorder.AudioSource.MIC
                        val retry = openRecord(
                            currentSource, currentInfo.sampleRate,
                            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer.size
                        )
                        if (retry != null) {
                            retry.startRecording()
                            synchronized(lock) {
                                try { current.record.stop() } catch (_: IllegalStateException) {}
                                current.record.release()
                                record = retry
                            }
                            current = current.copy(record = retry)
                            currentInfo = current.info.copy(sourceLabel = "MIC (fallback)")
                        }
                    }
                } else {
                    silentMs = 0f
                }
                val rms = kotlin.math.sqrt(sum / read)
                val level = (rms * LEVEL_GAIN).toFloat().coerceIn(0f, 1f)
                synchronized(clipLock) {
                    val iterator = captures.entries.iterator()
                    while (iterator.hasNext()) {
                        val entry = iterator.next()
                        val st = entry.value
                        val n = minOf(read, st.buffer.size - st.fill)
                        if (n > 0) {
                            System.arraycopy(buffer, 0, st.buffer, st.fill, n)
                            st.fill += n
                        }
                        if (st.fill >= st.buffer.size) {
                            iterator.remove()
                            finalizeCapture(entry.key, st)
                        }
                    }
                }
                val routedNow = routedLabel(rec.routedDevice)
                if (routedNow != currentInfo.routedDeviceName) {
                    currentInfo = currentInfo.copy(routedDeviceName = routedNow)
                }
                synchronized(lock) { track }?.write(buffer, 0, read)
                onLevel(level, currentInfo)
            }
            closeSetup(current)
            if (current.isBluetooth) linkController(null)
        }.apply { start() }
    }

    fun switch(config: Config) {
        pending = config
    }

    fun stop() {
        running = false
        pending = null
        worker?.join(JOIN_TIMEOUT_MS)
        worker = null
        finalizeAllCaptures()
        synchronized(lock) {
            record = null
            track = null
        }
    }

    private fun Config.isBluetoothDevice(): Boolean =
        device != null && (
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                device.type == AudioDeviceInfo.TYPE_BLE_HEADSET
            )

    @SuppressLint("MissingPermission")
    private fun openSetup(cfg: Config): Setup? {
        val isBt = cfg.isBluetoothDevice()
        var linkOk: Boolean? = null
        if (isBt) {
            linkOk = linkController(cfg.device)
        }
        val rate = if (isBt) 16_000 else 48_000
        val source = if (isBt) MediaRecorder.AudioSource.VOICE_COMMUNICATION else MediaRecorder.AudioSource.MIC
        val rec = openRecord(source, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 2048)
        if (rec == null) {
            if (isBt) linkController(null)
            return null
        }
        val preferred = if (cfg.device != null) rec.setPreferredDevice(cfg.device) else false
        rec.startRecording()
        var track: AudioTrack? = null
        var monitorName: String? = null
        val monitorOut = cfg.monitorOutput
        if (monitorOut != null) {
            val trackBuffer = AudioTrack.getMinBufferSize(
                rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            if (trackBuffer > 0) {
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(rate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(trackBuffer)
                    .build()
                track.setPreferredDevice(monitorOut)
                track.play()
                monitorName = monitorOut.productName?.toString().orEmpty().ifBlank { "output" }
            }
        }
        return Setup(
            record = rec,
            track = track,
            info = SessionInfo(
                sampleRate = rate,
                channelCount = 1,
                preferredDeviceApplied = preferred,
                sourceLabel = sourceLabel(source),
                monitorOutputName = monitorName,
                linkConfirmed = linkOk,
                routedDeviceName = routedLabel(rec.routedDevice)
            ),
            isBluetooth = isBt
        )
    }

    private fun routedLabel(device: AudioDeviceInfo?): String? = device?.let {
        it.productName?.toString().orEmpty().ifBlank { it.type.audioTypeLabel }
    }

    private fun sourceLabel(source: Int): String =
        if (source == MediaRecorder.AudioSource.VOICE_COMMUNICATION) "VOICE_COMMUNICATION" else "MIC"

    private fun closeSetup(setup: Setup) {
        synchronized(lock) {
            try { setup.record.stop() } catch (_: IllegalStateException) {}
            setup.record.release()
            setup.track?.run {
                try { stop() } catch (_: IllegalStateException) {}
                release()
            }
            if (record === setup.record) record = null
            if (track === setup.track) track = null
        }
    }

    @SuppressLint("MissingPermission")
    private fun openRecord(
        source: Int,
        sampleRate: Int,
        channelMask: Int,
        encoding: Int,
        minBuffer: Int
    ): AudioRecord? = try {
        val rec = AudioRecord(source, sampleRate, channelMask, encoding, minBuffer * 2)
        if (rec.state == AudioRecord.STATE_INITIALIZED) rec else {
            rec.release()
            null
        }
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val LEVEL_GAIN = 6f
        const val JOIN_TIMEOUT_MS = 1_500L
        const val SILENCE_PEAK_THRESHOLD = 40
        const val SILENCE_FALLBACK_MS = 1_500f
    }
}
