package com.liuh886.microuter.data

import com.liuh886.microuter.core.model.RouteEvent
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

class RouteEventLogger(private val capacity: Int = DEFAULT_CAPACITY) {

    private val buffer = ArrayDeque<RouteEvent>(capacity)
    private val format = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    @Synchronized
    fun log(event: RouteEvent) {
        if (buffer.size >= capacity) buffer.removeFirst()
        buffer.addLast(event)
    }

    @Synchronized
    fun events(): List<RouteEvent> = buffer.toList()

    @Synchronized
    fun export(): String = buildString {
        appendLine("MicRouter route log")
        appendLine("Generated: ${Date()}")
        appendLine()
        buffer.forEach { event ->
            appendLine("${format.format(Date(event.timestampMs))}  ${event.kindLabel}: ${event.message}")
        }
    }

    @Synchronized
    fun clear() = buffer.clear()

    private companion object {
        const val DEFAULT_CAPACITY = 500
    }
}
