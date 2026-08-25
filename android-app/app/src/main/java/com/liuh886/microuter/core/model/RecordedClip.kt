package com.liuh886.microuter.core.model

import kotlin.math.abs
import kotlin.math.sqrt

data class RecordedClip(
    val slot: Char,
    val deviceId: Int,
    val deviceName: String,
    val samples: ShortArray,
    val sampleRate: Int,
    val peak: Int,
    val rms: Float
) {
    val durationMs: Long
        get() = if (sampleRate <= 0) 0L else samples.size * 1000L / sampleRate

    override fun equals(other: Any?): Boolean =
        other is RecordedClip && other.slot == slot && other.samples.contentEquals(samples)

    override fun hashCode(): Int = slot.hashCode() * 31 + samples.contentHashCode()

    companion object {
        const val MAX_SECONDS = 15

        fun finalize(
            slot: Char,
            deviceId: Int,
            deviceName: String,
            buffer: ShortArray,
            fill: Int,
            sampleRate: Int
        ): RecordedClip {
            var peak = 0
            var sum = 0.0
            val n = fill.coerceAtMost(buffer.size)
            for (i in 0 until n) {
                val s = buffer[i].toInt()
                val a = abs(s)
                if (a > peak) peak = a
                val v = s / 32768.0
                sum += v * v
            }
            val rms = if (n == 0) 0f else sqrt(sum / n).toFloat()
            return RecordedClip(
                slot = slot,
                deviceId = deviceId,
                deviceName = deviceName,
                samples = buffer.copyOf(n),
                sampleRate = sampleRate,
                peak = peak,
                rms = rms
            )
        }
    }
}
