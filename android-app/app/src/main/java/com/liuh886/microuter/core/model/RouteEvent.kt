package com.liuh886.microuter.core.model

import java.util.concurrent.atomic.AtomicLong

enum class RouteEventKind {
    DEVICE_ADDED,
    DEVICE_REMOVED,
    MODE_CHANGED,
    COMMUNICATION_DEVICE_CHANGED
}

data class RouteEvent(
    val seq: Long,
    val timestampMs: Long,
    val kind: RouteEventKind,
    val message: String
) {
    val kindLabel: String
        get() = when (kind) {
            RouteEventKind.DEVICE_ADDED -> "Device added"
            RouteEventKind.DEVICE_REMOVED -> "Device removed"
            RouteEventKind.MODE_CHANGED -> "Audio mode changed"
            RouteEventKind.COMMUNICATION_DEVICE_CHANGED -> "Communication device changed"
        }

    companion object {
        private val counter = AtomicLong(0)

        fun create(timestampMs: Long, kind: RouteEventKind, message: String): RouteEvent =
            RouteEvent(counter.incrementAndGet(), timestampMs, kind, message)
    }
}
