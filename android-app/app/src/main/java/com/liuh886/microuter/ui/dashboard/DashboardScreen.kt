package com.liuh886.microuter.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.liuh886.microuter.core.model.AudioDeviceItem
import com.liuh886.microuter.data.AudioRepository
import com.liuh886.microuter.ui.components.CheckTrailing
import com.liuh886.microuter.ui.components.DeviceGlyph
import com.liuh886.microuter.ui.components.GroupHeader
import com.liuh886.microuter.ui.components.ListRow
import com.liuh886.microuter.ui.components.PillAction
import com.liuh886.microuter.ui.components.StatusPill
import com.liuh886.microuter.ui.theme.GlassCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: AudioRepository) : ViewModel() {

    private val _status = MutableStateFlow(repository.state.value)
    val status = _status.asStateFlow()

    init {
        repository.refresh()
        viewModelScope.launch {
            repository.state.collect { _status.value = it }
        }
    }

    fun select(device: AudioDeviceItem) {
        repository.selectCommunicationDevice(device.id)
    }

    fun clear() {
        repository.clearCommunicationSelection()
    }
}

@Composable
fun DashboardScreen(repository: AudioRepository) {
    val viewModel: DashboardViewModel = viewModel { DashboardViewModel(repository) }
    val state by viewModel.status.collectAsStateWithLifecycle()
    val commDevice = state.communicationDevice

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "MicRouter",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
            )
        }
        item {
            GlassCard {
                ListRow(
                    title = "Audio Mode",
                    subtitle = if (state.isBluetoothCommunication) "Bluetooth owns call audio" else null,
                    showDivider = true
                ) {
                    StatusPill(
                        text = state.modeLabel.substringBefore(' '),
                        color = if (state.isBluetoothCommunication) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                ListRow(
                    title = commDevice?.name ?: "System Default",
                    subtitle = commDevice?.typeLabel ?: "Android is choosing automatically",
                    leading = { commDevice?.let { DeviceGlyph(it.type) } },
                    showDivider = false
                ) {
                    ValuePill(active = commDevice != null)
                }
            }
        }
        item { GroupHeader("Inputs") }
        item {
            GlassCard {
                state.inputs.forEachIndexed { index, device ->
                    ListRow(
                        title = device.name,
                        subtitle = device.typeLabel,
                        leading = { DeviceGlyph(device.type) },
                        showDivider = index < state.inputs.lastIndex,
                        onClick = if (device.isCommunicationCandidate) {
                            { viewModel.select(device) }
                        } else {
                            null
                        }
                    ) {
                        if (device.isCommunicationCandidate) {
                            PillAction("Use") { viewModel.select(device) }
                        } else if (device.id == commDevice?.id) {
                            CheckTrailing()
                        }
                    }
                }
            }
        }
        item { GroupHeader("Outputs") }
        item {
            GlassCard {
                state.outputs.forEachIndexed { index, device ->
                    val isActive = commDevice?.id == device.id
                    ListRow(
                        title = device.name,
                        subtitle = device.typeLabel,
                        leading = { DeviceGlyph(device.type) },
                        showDivider = index < state.outputs.lastIndex,
                        onClick = if (device.isCommunicationCandidate) {
                            { viewModel.select(device) }
                        } else {
                            null
                        }
                    ) {
                        if (isActive) {
                            CheckTrailing()
                        } else if (device.isCommunicationCandidate) {
                            PillAction("Use") { viewModel.select(device) }
                        }
                    }
                }
            }
        }
        item {
            Box(Modifier.fillParentMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = { viewModel.clear() }) {
                    Text("Reset to System Default")
                }
            }
        }
    }
}

@Composable
private fun ValuePill(active: Boolean) {
    StatusPill(
        text = if (active) "Active" else "Auto",
        color = if (active) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}
