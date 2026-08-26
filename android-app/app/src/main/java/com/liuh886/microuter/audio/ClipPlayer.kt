package com.liuh886.microuter.audio

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import com.liuh886.microuter.core.model.RecordedClip

class ClipPlayer {

    private var worker: Thread? = null
    private val lock = Any()
    private var track: AudioTrack? = null

    @Volatile
    private var playing = false

    val isPlaying: Boolean
        get() = playing

    fun play(clip: RecordedClip, output: AudioDeviceInfo?, onDone: () -> Unit, onError: (String) -> Unit) {
        stop()
        playing = true
        worker = Thread {
            var t: AudioTrack? = null
            try {
                val bytes = clip.samples.size * 2
                val format = AudioFormat.Builder()
                    .setSampleRate(clip.sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                t = try {
                    AudioTrack.Builder()
                        .setAudioAttributes(attrs)
                        .setAudioFormat(format)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .setBufferSizeInBytes(bytes)
                        .build()
                } catch (_: Exception) {
                    null
                }

                if (t != null && t.state != AudioTrack.STATE_INITIALIZED) {
                    t.release()
                    t = null
                }

                val useStatic = t != null
                if (t == null) {
                    val bufSize = AudioTrack.getMinBufferSize(
                        clip.sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
                    )
                    if (bufSize <= 0) {
                        onError("Playback unsupported on this device")
                        return@Thread
                    }
                    t = AudioTrack.Builder()
                        .setAudioAttributes(attrs)
                        .setAudioFormat(format)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .setBufferSizeInBytes(bufSize)
                        .build()
                }
                val player = t
                player.setPreferredDevice(output)
                val startMs = SystemClock.elapsedRealtime()
                val deadlineMs = startMs + clip.durationMs.coerceAtLeast(1L) + COMPLETION_MARGIN_MS

                if (useStatic) {
                    player.write(clip.samples, 0, clip.samples.size)
                    player.play()
                    synchronized(lock) { track = player }
                    while (playing && SystemClock.elapsedRealtime() < deadlineMs) {
                        Thread.sleep(HEAD_POLL_MS)
                    }
                } else {
                    player.play()
                    synchronized(lock) { track = player }
                    var offset = 0
                    while (playing && offset < clip.samples.size) {
                        val n = player.write(
                            clip.samples, offset,
                            minOf(2048, clip.samples.size - offset)
                        )
                        if (n < 0) {
                            onError("Playback failed")
                            return@Thread
                        }
                        offset += n
                    }
                    while (playing && SystemClock.elapsedRealtime() < deadlineMs) {
                        Thread.sleep(HEAD_POLL_MS)
                    }
                }
                if (playing) onDone()
            } catch (_: Exception) {
                onError("Playback failed")
            } finally {
                synchronized(lock) {
                    try { t?.stop() } catch (_: IllegalStateException) {}
                    t?.release()
                    track = null
                }
                playing = false
            }
        }.apply { start() }
    }

    fun stop() {
        playing = false
        worker?.join(JOIN_TIMEOUT_MS)
        worker = null
    }

    private companion object {
        const val JOIN_TIMEOUT_MS = 2_000L
        const val HEAD_POLL_MS = 40L
        const val COMPLETION_MARGIN_MS = 150L
    }
}
