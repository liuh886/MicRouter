package com.liuh886.microuter.data

import android.content.Context
import android.os.Build
import com.liuh886.microuter.core.model.AudioSessionState
import java.util.Date

class DiagnosticReportBuilder(private val context: Context) {

    fun build(state: AudioSessionState, routeLog: String): String = buildString {
        appendLine("MicRouter diagnostic report")
        appendLine("Generated: ${Date()}")
        appendLine()
        appendSection("Device")
        appendLine("Manufacturer: ${Build.MANUFACTURER}")
        appendLine("Model: ${Build.MODEL}")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("App version: ${appVersion()}")
        appendLine()
        appendSection("Session snapshot")
        appendLine("Audio mode: ${state.modeLabel}")
        appendLine(
            "Communication device: " +
                (state.communicationDevice?.let { "${it.name} (${it.typeLabel})" } ?: "system default")
        )
        appendLine()
        appendSection("Input devices")
        state.inputs.forEach {
            val flag = if (it.isCommunicationCandidate) " [communication candidate]" else ""
            appendLine("- ${it.name} [${it.typeLabel}]$flag")
        }
        appendLine()
        appendSection("Output devices")
        state.outputs.forEach { appendLine("- ${it.name} [${it.typeLabel}]") }
        appendLine()
        appendSection("Route log")
        appendLine(routeLog)
    }

    private fun StringBuilder.appendSection(title: String) {
        appendLine("## $title")
    }

    private fun appVersion(): String = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.versionName ?: "unknown"
    } catch (_: Exception) {
        "unknown"
    }
}
