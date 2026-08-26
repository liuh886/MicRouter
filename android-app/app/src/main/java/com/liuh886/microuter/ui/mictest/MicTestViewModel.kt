package com.liuh886.microuter.ui.mictest

import android.media.AudioDeviceInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liuh886.microuter.audio.ClipPlayer
import com.liuh886.microuter.audio.MicTester
import com.liuh886.microuter.core.model.AudioDeviceItem
import com.liuh886.microuter.core.model.RecordedClip
import com.liuh886.microuter.data.AudioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MicTestViewModel(
    private val repository: AudioRepository,
    private val tester: MicTester,
    private val clipPlayer: ClipPlayer
) : ViewModel() {

    data class UiState(
        val inputs: List<AudioDeviceItem> = emptyList(),
        val outputs: List<AudioDeviceItem> = emptyList(),
        val selected: AudioDeviceItem? = null,
        val level: Float = 0f,
        val running: Boolean = false,
        val sessionInfo: MicTester.SessionInfo? = null,
        val error: String? = null,
        val earMonitor: Boolean = false,
        val monitorName: String? = null,
        val modeLabel: String = "NORMAL",
        val systemComm: AudioDeviceItem? = null,
        val btMicLinkUp: Boolean = false,
        val monitorOutputId: Int? = null,
        val clipA: RecordedClip? = null,
        val clipB: RecordedClip? = null,
        val captureSlot: Char? = null,
        val captureSeconds: Int = 0,
        val playingSlot: Char? = null
    )

    private val _uiState = MutableStateFlow(UiState(selected = repository.selectedInput.value))
    val uiState = _uiState.asStateFlow()

    init {
        if (repository.selectedInput.value == null) {
            repository.state.value.inputs
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
                ?.let { repository.selectInput(it) }
        }
        viewModelScope.launch {
            repository.state.collect { state ->
                _uiState.update { current ->
                    current.copy(
                        inputs = state.inputs,
                        outputs = state.outputs,
                        modeLabel = state.modeLabel,
                        systemComm = state.communicationDevice,
                        btMicLinkUp = state.btMicLinkUp
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.selectedInput.collect { device ->
                if (device != null) {
                    _uiState.update { it.copy(selected = device, error = null) }
                    if (_uiState.value.running) {
                        repository.resolveInputDevice(device.id)?.let { info ->
                            tester.switch(MicTester.Config(info, resolveMonitor()))
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            tester.clipReady.collect { clip ->
                _uiState.update { current ->
                    current.copy(
                        clipA = if (clip.slot == 'A') clip else current.clipA,
                        clipB = if (clip.slot == 'B') clip else current.clipB,
                        captureSlot = if (current.captureSlot == clip.slot) null else current.captureSlot
                    )
                }
            }
        }
    }

    fun selectDevice(device: AudioDeviceItem) {
        repository.selectInput(device)
    }

    fun selectOutput(device: AudioDeviceItem) {
        repository.selectCommunicationDevice(device.id)
    }

    fun selectMonitorOutput(device: AudioDeviceItem) {
        _uiState.update { it.copy(monitorOutputId = device.id, monitorName = device.name) }
        if (_uiState.value.running) {
            val input = _uiState.value.selected?.let { repository.resolveInputDevice(it.id) }
            if (input != null) {
                tester.switch(MicTester.Config(input, resolveMonitor()))
            }
        }
    }

    fun isMonitorCapable(device: AudioDeviceItem): Boolean =
        device.type in MONITOR_TYPES || device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER

    fun toggleRun() {
        if (_uiState.value.running) {
            tester.stop()
            repository.endLink()
            _uiState.update { it.copy(running = false, level = 0f, sessionInfo = null, captureSlot = null) }
            return
        }
        val selected = _uiState.value.selected
            ?: _uiState.value.inputs.firstOrNull { it.isSource }?.also { selectDevice(it) }
        if (selected == null) {
            _uiState.value = _uiState.value.copy(error = "No input device available")
            return
        }
        val device = repository.resolveInputDevice(selected.id)
        if (device == null) {
            _uiState.value = _uiState.value.copy(error = "Device is no longer available")
            return
        }
        val monitor = if (_uiState.value.earMonitor) resolveMonitor() else null
        _uiState.update { current ->
            current.copy(
                running = true,
                error = null,
                level = 0f,
                sessionInfo = null,
                monitorName = if (monitor != null) monitorCandidate()?.name else null
            )
        }
        tester.start(
            MicTester.Config(device, monitor),
            onLevel = { level, info ->
                _uiState.update { cur ->
                    cur.copy(
                        level = level,
                        sessionInfo = info,
                        captureSeconds = if (cur.captureSlot != null) {
                            tester.clipElapsedMs(cur.captureSlot) / 1000
                        } else {
                            cur.captureSeconds
                        }
                    )
                }
            },
            onError = { msg ->
                _uiState.update { it.copy(error = msg, running = false, sessionInfo = null) }
            }
        )
    }

    fun toggleCapture(slot: Char) {
        val current = _uiState.value
        if (current.captureSlot == slot) {
            tester.endCapture(slot)
            _uiState.update { it.copy(captureSlot = null) }
            return
        }
        if (!current.running) {
            _uiState.update { it.copy(error = "Press Start first") }
            return
        }
        current.captureSlot?.let { tester.endCapture(it) }
        val device = current.selected?.let { repository.resolveInputDevice(it.id) }
        if (device == null) {
            _uiState.update { it.copy(error = "Device is no longer available") }
            return
        }
        val name = current.selected?.name ?: "Input"
        val ok = tester.beginCapture(slot, device.id, name)
        _uiState.update {
            if (ok) it.copy(captureSlot = slot, error = null)
            else it.copy(error = "Cannot start capture")
        }
    }

    fun playSlot(slot: Char) {
        val playingNow = _uiState.value.playingSlot
        if (playingNow != null) {
            clipPlayer.stop()
            _uiState.update { it.copy(playingSlot = null) }
            if (playingNow == slot) return
        }
        val clip = (if (slot == 'A') _uiState.value.clipA else _uiState.value.clipB) ?: return
        _uiState.update { it.copy(playingSlot = slot) }
        clipPlayer.play(
            clip,
            resolveMonitor(),
            onDone = { _uiState.update { it.copy(playingSlot = null) } },
            onError = { msg ->
                _uiState.update { it.copy(playingSlot = null, error = msg) }
            }
        )
    }

    fun setEarMonitor(enabled: Boolean) {
        _uiState.update { current ->
            current.copy(
                earMonitor = enabled,
                monitorName = if (enabled) monitorCandidate()?.name else null
            )
        }
        if (_uiState.value.running) {
            val device = _uiState.value.selected?.let { repository.resolveInputDevice(it.id) }
            if (device != null) {
                tester.switch(MicTester.Config(device, if (enabled) resolveMonitor() else null))
            }
        }
    }

    private fun resolveMonitor(): AudioDeviceInfo? {
        val id = monitorCandidate()?.id ?: return null
        return repository.resolveInputDevice(id)
    }

    private fun monitorCandidate(): AudioDeviceItem? {
        val outputs = _uiState.value.outputs
        _uiState.value.monitorOutputId?.let { chosen ->
            outputs.firstOrNull { it.id == chosen }?.let { return it }
        }
        return outputs.firstOrNull { it.type in MONITOR_TYPES }
            ?: outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
    }

    override fun onCleared() {
        tester.stop()
        clipPlayer.stop()
        repository.endLink()
        super.onCleared()
    }

    companion object {
        val MONITOR_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE
        )
    }
}
