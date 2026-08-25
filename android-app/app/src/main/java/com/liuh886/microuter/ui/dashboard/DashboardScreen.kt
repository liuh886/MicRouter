package com.liuh886.microuter.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.liuh886.microuter.core.model.AudioDeviceItem
import com.liuh886.microuter.data.AudioRepository
import com.liuh886.microuter.ui.components.Chevron
import com.liuh886.microuter.ui.components.DeviceGlyph
import com.liuh886.microuter.ui.components.DevicePickerSheet
import com.liuh886.microuter.ui.components.GroupHeader
import com.liuh886.microuter.ui.components.ListRow
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

    fun chooseInput(device: AudioDeviceItem) {
        repository.selectInput(device)
        if (device.isCommunicationCandidate) {
            repository.selectCommunicationDevice(device.id)
        }
    }

    fun chooseOutput(device: AudioDeviceItem) {
        repository.selectCommunicationDevice(device.id)
    }

    fun clear() {
        repository.clearCommunicationSelection()
    }
}

@Composable
fun DashboardScreen(repository: AudioRepository, onOpenLog: () -> Unit = {}) {
    val viewModel: DashboardViewModel = viewModel { DashboardViewModel(repository) }
    val state by viewModel.status.collectAsStateWithLifecycle()
    val selectedInput by repository.selectedInput.collectAsStateWithLifecycle()
    var showInputSheet by remember { mutableStateOf(false) }
    var showOutputSheet by remember { mutableStateOf(false) }
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
                    title = "Input",
                    subtitle = selectedInput?.typeLabel ?: "Tap to choose test input",
                    leading = { selectedInput?.let { DeviceGlyph(it.type) } },
                    showDivider = true,
                    onClick = { showInputSheet = true }
                ) {
                    Text(
                        selectedInput?.name ?: "Choose",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Chevron()
                }
                ListRow(
                    title = "Output · Calls",
                    subtitle = commDevice?.typeLabel ?: "Android chooses automatically",
                    leading = { commDevice?.let { DeviceGlyph(it.type) } },
                    showDivider = true,
                    onClick = { showOutputSheet = true }
                ) {
                    Text(
                        commDevice?.name ?: "Auto",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Chevron()
                }
                ListRow(
                    title = "Live Status",
                    subtitle = "Mode: ${state.modeLabel} · BT mic link ${if (state.btMicLinkUp) "UP" else "down"}",
                    showDivider = false
                ) {
                    StatusPill(
                        text = if (state.btMicLinkUp) "SCO UP" else "NO LINK",
                        color = if (state.btMicLinkUp) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
        item {
            Row(
                Modifier.fillParentMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = { viewModel.clear() }) {
                    Text("Reset to System Default")
                }
                TextButton(onClick = onOpenLog) {
                    Text("Route log →")
                }
            }
        }
        item { GroupHeader("All Devices") }
        item {
            GlassCard {
                val all = state.inputs.filter { it.isSource } + state.outputs.filter { it.isSink }
                all.forEachIndexed { index, device ->
                    ListRow(
                        title = device.name,
                        subtitle = device.typeLabel +
                            if (device.isCommunicationCandidate) " · controllable" else "",
                        leading = { DeviceGlyph(device.type) },
                        showDivider = index < all.lastIndex
                    ) {
                        if (device.id == commDevice?.id) {
                            StatusPill("Active", MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }
    }
    if (showInputSheet) {
        DevicePickerSheet(
            title = "Choose Input",
            devices = state.inputs.filter { it.isSource },
            selectedId = selectedInput?.id,
            allowPick = { it.recordable },
            disabledTag = "not recordable",
            allowedTag = "sets call route",
            onPick = { device ->
                viewModel.chooseInput(device)
                showInputSheet = false
            },
            onDismiss = { showInputSheet = false }
        )
    }
    if (showOutputSheet) {
        DevicePickerSheet(
            title = "Call Output Route",
            devices = state.outputs,
            selectedId = commDevice?.id,
            allowPick = { it.isCommunicationCandidate },
            disabledTag = "fixed",
            allowedTag = "call route",
            onPick = { device ->
                viewModel.chooseOutput(device)
                showOutputSheet = false
            },
            onDismiss = { showOutputSheet = false }
        )
    }
}
