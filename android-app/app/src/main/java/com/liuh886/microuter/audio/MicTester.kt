package com.liuh886.microuter.audio

import android.annotation.SuppressLint
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

class MicTester {

    data class SessionInfo(val sampleRate: Int, val channelCount: Int, val preferredDeviceApplied: Boolean)

    private var record: AudioRecord? = null
    private var worker: Thread? = null

    @Volatile
    private var running = false

    val isRunning: Boolean
        get() = running

    @SuppressLint("MissingPermission")
    fun start(
        device: AudioDeviceInfo?,
        source: Int = MediaRecorder.AudioSource.VOICE_COMMUNICATION,
        onLevel: (Float, SessionInfo) -> Unit
    ): SessionInfo? {
        stop()
        val sampleRate = 48_000
        val channelMask = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelMask, encoding)
        if (minBuffer <= 0) return null
        val rec = AudioRecord(source, sampleRate, channelMask, encoding, minBuffer * 2)
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return null
        }
        val preferredApplied = if (device != null) rec.setPreferredDevice(device) else false
        rec.startRecording()
        record = rec
        running = true
        val info = SessionInfo(sampleRate, 1, preferredApplied)
        worker = Thread {
            val buffer = ShortArray(minBuffer)
            while (running) {
                val read = rec.read(buffer, 0, buffer.size)
                if (read <= 0) continue
                var sum = 0.0
                for (i in 0 until read) {
                    val v = buffer[i] / 32768.0
                    sum += v * v
                }
                val rms = kotlin.math.sqrt(sum / read)
                val level = (rms * LEVEL_GAIN).toFloat().coerceIn(0f, 1f)
                onLevel(level, info)
            }
        }.apply { start() }
        return info
    }

    fun stop() {
        running = false
        worker?.join(JOIN_TIMEOUT_MS)
        worker = null
        record?.run {
            try {
                stop()
            } catch (_: IllegalStateException) {
            }
            release()
        }
        record = null
    }

    private companion object {
        const val LEVEL_GAIN = 6f
        const val JOIN_TIMEOUT_MS = 1_000L
    }
}
