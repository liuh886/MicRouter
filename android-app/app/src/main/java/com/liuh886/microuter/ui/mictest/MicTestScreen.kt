package com.liuh886.microuter.ui.mictest

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liuh886.microuter.audio.MicTester
import com.liuh886.microuter.core.model.AudioDeviceItem
import com.liuh886.microuter.data.AudioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
                _uiState.value = _uiState.value.copy(
                    inputs = state.inputs,
                    selected = _uiState.value.selected?.let { sel ->
                        state.inputs.firstOrNull { it.id == sel.id }
                    }
                )
            }
        }
    }

    fun select(device: AudioDeviceItem) {
        _uiState.value = _uiState.value.copy(selected = device)
    }

    fun toggle() {
        if (tester.isRunning) {
            tester.stop()
            _uiState.value = _uiState.value.copy(running = false, level = 0f, sessionInfo = null)
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
        val info = tester.start(device) { level, session ->
            _uiState.value = _uiState.value.copy(level = level, sessionInfo = session)
        }
        _uiState.value = if (info != null) {
            _uiState.value.copy(running = true, error = null, sessionInfo = info)
        } else {
            _uiState.value.copy(error = "Failed to open AudioRecord")
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

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Microphone test", style = MaterialTheme.typography.titleLarge)
        if (!micPermissionGranted) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Microphone permission required", style = MaterialTheme.typography.titleSmall)
                    Button(onClick = onRequestMicPermission) { Text("Grant permission") }
                }
            }
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        LevelMeter(level = state.level, enabled = state.running)
        state.sessionInfo?.let {
            Text(
                "${it.sampleRate} Hz · ${it.channelCount} ch · preferred device ${if (it.preferredDeviceApplied) "applied" else "not applied"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = { viewModel.toggle() },
            enabled = micPermissionGranted
        ) {
            Text(if (state.running) "Stop" else "Start")
        }
        Text("Input devices", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(state.inputs, key = { it.id }) { device ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.select(device) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(device.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                device.typeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (state.selected?.id == device.id) {
                            Text(
                                "Selected",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelMeter(level: Float, enabled: Boolean) {
    val animated by animateFloatAsState(targetValue = if (enabled) level else 0f, label = "level")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(10.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated.coerceIn(0f, 1f))
                .height(20.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
        )
    }
}
