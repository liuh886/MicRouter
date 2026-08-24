package com.liuh886.microuter.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.liuh886.microuter.core.model.AudioSessionState
import com.liuh886.microuter.core.model.RouteEvent
import com.liuh886.microuter.core.model.RouteEventKind
import com.liuh886.microuter.core.model.audioTypeLabel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class RouteMonitor(context: Context) {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val mainExecutor = ContextCompat.getMainExecutor(context)

    private val _events = MutableSharedFlow<RouteEvent>(
        replay = REPLAY_CAPACITY,
        extraBufferCapacity = REPLAY_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<RouteEvent> = _events

    private var started = false

    private fun emit(kind: RouteEventKind, message: String) {
        _events.tryEmit(RouteEvent(System.currentTimeMillis(), kind, message))
    }

    private fun formatDevice(device: AudioDeviceInfo): String {
        val name = device.productName?.toString().orEmpty().ifBlank { "Unknown" }
        return "$name (${device.type.audioTypeLabel})"
    }

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            addedDevices.forEach { device ->
                emit(RouteEventKind.DEVICE_ADDED, formatDevice(device))
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            removedDevices.forEach { device ->
                emit(RouteEventKind.DEVICE_REMOVED, formatDevice(device))
            }
        }
    }

    private val modeListener = AudioManager.OnModeChangedListener { mode ->
        emit(RouteEventKind.MODE_CHANGED, AudioSessionState.labelForMode(mode))
    }

    private val communicationDeviceListener =
        AudioManager.OnCommunicationDeviceChangedListener { device ->
            emit(
                RouteEventKind.COMMUNICATION_DEVICE_CHANGED,
                device?.let(::formatDevice) ?: "cleared (system default)"
            )
        }

    fun start() {
        if (started) return
        started = true
        audioManager.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
        audioManager.addOnModeChangedListener(mainExecutor, modeListener)
        audioManager.addOnCommunicationDeviceChangedListener(mainExecutor, communicationDeviceListener)
    }

    fun stop() {
        if (!started) return
        started = false
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
        audioManager.removeOnModeChangedListener(modeListener)
        audioManager.removeOnCommunicationDeviceChangedListener(communicationDeviceListener)
    }

    private companion object {
        const val REPLAY_CAPACITY = 64
    }
}
