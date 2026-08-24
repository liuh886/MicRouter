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
import com.liuh886.microuter.core.model.RouteEventKind
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
    private val reportBuilder = DiagnosticReportBuilder(appContext)

    private val deviceIndex = mutableMapOf<Int, AudioDeviceInfo>()

    private val _state = MutableStateFlow(AudioSessionState())
    val state: StateFlow<AudioSessionState> = _state.asStateFlow()

    private val _events = MutableStateFlow<List<RouteEvent>>(emptyList())
    val events: StateFlow<List<RouteEvent>> = _events.asStateFlow()

    private val _selectedInput = MutableStateFlow<AudioDeviceItem?>(null)
    val selectedInput: StateFlow<AudioDeviceItem?> = _selectedInput.asStateFlow()

    fun selectInput(item: AudioDeviceItem) {
        _selectedInput.value = item
    }

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
            outputs = scanner.outputDevices().map { it.toItem(candidateIds.contains(it.id)) },
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

    fun diagnosticReport(): String =
        reportBuilder.build(refresh(), logger.export())

    fun beginLink(device: AudioDeviceInfo): Boolean {
        controller.setMode(AudioManager.MODE_IN_COMMUNICATION)
        val selected = controller.select(device)
        if (!selected) {
            controller.setMode(AudioManager.MODE_NORMAL)
            return false
        }
        val isBtTarget = device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            device.type == AudioDeviceInfo.TYPE_BLE_HEADSET
        if (!isBtTarget) return true
        val deadline = android.os.SystemClock.elapsedRealtime() + LINK_CONFIRM_TIMEOUT_MS
        while (android.os.SystemClock.elapsedRealtime() < deadline) {
            val current = controller.current()
            if (current != null && (current.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    current.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
            ) {
                return true
            }
            Thread.sleep(100)
        }
        return false
    }

    fun endLink() {
        controller.clear()
        controller.setMode(AudioManager.MODE_NORMAL)
    }

    private fun mode(): Int =
        appContext.getSystemService(AudioManager::class.java).mode

    private companion object {
        const val LINK_CONFIRM_TIMEOUT_MS = 2_000L
    }
}
