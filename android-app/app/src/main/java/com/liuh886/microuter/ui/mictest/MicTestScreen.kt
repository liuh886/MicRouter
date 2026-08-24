package com.liuh886.microuter.ui.mictest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liuh886.microuter.audio.MicTester
import com.liuh886.microuter.core.model.AudioDeviceItem
import com.liuh886.microuter.data.AudioRepository
import com.liuh886.microuter.ui.components.CapsuleButton
import com.liuh886.microuter.ui.components.CheckTrailing
import com.liuh886.microuter.ui.components.DeviceGlyph
import com.liuh886.microuter.ui.components.GroupHeader
import com.liuh886.microuter.ui.components.LevelTrack
import com.liuh886.microuter.ui.components.ListRow
import com.liuh886.microuter.ui.theme.GlassCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MicTestViewModel(
    private val repository: AudioRepository,
    private val tester: MicTester
) : ViewModel() {

    data class UiState(
        val inputs: List<AudioDeviceItem> = emptyList(),
        val selected: AudioDeviceItem? = null,
        val level: Float = 0f,
        val running: Boolean = false,
        val sessionInfo: MicTester.SessionInfo? = null,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState(inputs = repository.state.value.inputs))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.state.collect { state ->
                _uiState.update { current ->
                    current.copy(
                        inputs = state.inputs,
                        selected = current.selected?.let { sel ->
                            state.inputs.firstOrNull { it.id == sel.id }
                        }
                    )
                }
            }
        }
    }

    fun select(device: AudioDeviceItem) {
        _uiState.update { it.copy(selected = device, error = null) }
    }

    fun toggle() {
        if (tester.isRunning) {
            tester.stop()
            _uiState.update { it.copy(running = false, level = 0f, sessionInfo = null) }
            return
        }
        val selectedId = _uiState.value.selected?.id ?: run {
            _uiState.value = _uiState.value.copy(error = "Select an input device first")
            return
        }
        val device = repository.resolveInputDevice(selectedId)
        if (device == null) {
            _uiState.value = _uiState.value.copy(error = "Device is no longer available")
            return
        }
        val info = try {
            tester.start(device) { level, session ->
                _uiState.update { it.copy(level = level, sessionInfo = session) }
            }
        } catch (_: SecurityException) {
            null
        } catch (_: UnsupportedOperationException) {
            null
        }
        _uiState.update { current ->
            if (info != null) {
                current.copy(running = true, error = null, sessionInfo = info)
            } else {
                current.copy(running = false, error = "Failed to open AudioRecord for this device")
            }
        }
    }

    override fun onCleared() {
        tester.stop()
        super.onCleared()
    }
}

@Composable
fun MicTestScreen(
    repository: AudioRepository,
    tester: MicTester,
    micPermissionGranted: Boolean,
    onRequestMicPermission: () -> Unit
) {
    val viewModel: MicTestViewModel = viewModel { MicTestViewModel(repository, tester) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selected = state.selected

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Mic Test",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )
        if (!micPermissionGranted) {
            GlassCard(cornerRadius = 16.dp) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Microphone permission required", style = MaterialTheme.typography.titleSmall)
                    CapsuleButton("Grant Permission") { onRequestMicPermission() }
                }
            }
        }
        state.error?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        GlassCard(cornerRadius = 20.dp) {
            ListRow(
                title = selected?.name ?: "No input selected",
                subtitle = selected?.typeLabel ?: "Choose a device below",
                leading = { selected?.let { d -> DeviceGlyph(d.type) } },
                showDivider = true
            ) {
                if (state.running) {
                    RunningBadge()
                }
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Level", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${(state.level * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                LevelTrack(if (state.running) state.level else 0f)
            }
            state.sessionInfo?.let { info ->
                ListRow(
                    title = "Session",
                    subtitle = "${info.sampleRate} Hz · ${info.channelCount} ch · preferred device ${if (info.preferredDeviceApplied) "applied" else "rejected"}",
                    showDivider = false
                )
            } ?: ListRow(title = "Session", subtitle = "Idle", showDivider = false)
        }
        CapsuleButton(
            text = if (state.running) "Stop" else "Start",
            enabled = micPermissionGranted
        ) { viewModel.toggle() }
        GroupHeader("Input Devices")
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                GlassCard(cornerRadius = 20.dp) {
                    state.inputs.forEachIndexed { index, device ->
                        ListRow(
                            title = device.name,
                            subtitle = device.typeLabel,
                            leading = { DeviceGlyph(device.type) },
                            showDivider = index < state.inputs.lastIndex,
                            onClick = { viewModel.select(device) }
                        ) {
                            if (selected?.id == device.id) {
                                CheckTrailing()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RunningBadge() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Recording",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.padding(start = 4.dp))
        PulsingDot()
    }
}
