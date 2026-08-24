package com.liuh886.microuter.core.model

enum class RouteEventKind {
    DEVICE_ADDED,
    DEVICE_REMOVED,
    MODE_CHANGED,
    COMMUNICATION_DEVICE_CHANGED
}

data class RouteEvent(
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
}
