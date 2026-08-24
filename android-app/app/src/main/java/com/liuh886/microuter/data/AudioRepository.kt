package com.liuh886.microuter.data

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.liuh886.microuter.audio.AudioDeviceScanner
import com.liuh886.microuter.audio.CommunicationController
import com.liuh886.microuter.audio.RouteMonitor
import com.liuh886.microuter.core.model.AudioDeviceItem
import com.liuh886.microuter.core.model.AudioSessionState
import com.liuh886.microuter.core.model.RouteEvent
import com.liuh886.microuter.core.model.toItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioRepository(
    context: Context,
    private val scanner: AudioDeviceScanner,
    private val controller: CommunicationController,
    private val monitor: RouteMonitor,
    private val logger: RouteEventLogger,
    private val scope: CoroutineScope
) {
    private val appContext = context.applicationContext

    private val deviceIndex = mutableMapOf<Int, AudioDeviceInfo>()

    private val _state = MutableStateFlow(AudioSessionState())
    val state: StateFlow<AudioSessionState> = _state.asStateFlow()

    private val _events = MutableStateFlow<List<RouteEvent>>(emptyList())
    val events: StateFlow<List<RouteEvent>> = _events.asStateFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true
        refresh()
        scope.launch {
            monitor.events.collect { event ->
                logger.log(event)
                _events.value = logger.events().asReversed()
                if (event.kind == RouteEventKind.MODE_CHANGED ||
                    event.kind == RouteEventKind.COMMUNICATION_DEVICE_CHANGED ||
                    event.kind == RouteEventKind.DEVICE_ADDED ||
                    event.kind == RouteEventKind.DEVICE_REMOVED
                ) {
                    refresh()
                }
            }
        }
        monitor.start()
    }

    fun stop() {
        if (!started) return
        started = false
        monitor.stop()
    }

    @Synchronized
    fun refresh(): AudioSessionState {
        synchronized(deviceIndex) {
            deviceIndex.clear()
            scanner.allDevices().forEach { deviceIndex[it.id] = it }
        }
        val candidates = controller.candidates()
        val candidateIds = candidates.map { it.id }.toSet()
        val current = controller.current()
        val state = AudioSessionState(
            modeLabel = AudioSessionState.labelForMode(mode()),
            communicationDevice = current?.let { it.toItem(candidateIds.contains(it.id)) },
            inputs = scanner.inputDevices().map { it.toItem(candidateIds.contains(it.id)) },
            outputs = scanner.outputDevices().map { it.toItem(false) },
            communicationCandidates = candidates.map { it.toItem(true) }
        )
        _state.value = state
        return state
    }

    fun selectCommunicationDevice(itemId: Int): Boolean {
        val device = synchronized(deviceIndex) { deviceIndex[itemId] }
            ?: return false
        val result = controller.select(device)
        refresh()
        return result
    }

    fun clearCommunicationSelection() {
        controller.clear()
        refresh()
    }

    fun resolveInputDevice(itemId: Int): AudioDeviceInfo? =
        synchronized(deviceIndex) { deviceIndex[itemId] }

    fun exportLog(): String = logger.export()

    private fun mode(): Int =
        appContext.getSystemService(AudioManager::class.java).mode
}
