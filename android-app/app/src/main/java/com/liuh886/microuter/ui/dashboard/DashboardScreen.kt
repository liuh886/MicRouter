package com.liuh886.microuter.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.liuh886.microuter.core.model.AudioDeviceItem
import com.liuh886.microuter.data.AudioRepository
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Audio mode", style = MaterialTheme.typography.labelMedium)
                    Text(state.modeLabel, style = MaterialTheme.typography.titleLarge)
                    if (state.isBluetoothCommunication) {
                        Text(
                            "Bluetooth headset owns call audio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Active communication device", style = MaterialTheme.typography.labelMedium)
                    Text(
                        state.communicationDevice?.let { "${it.name} (${it.typeLabel})" } ?: "System default",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        item {
            Text("Inputs", style = MaterialTheme.typography.titleMedium)
        }
        items(state.inputs, key = { it.id }) { device ->
            DeviceRow(device) {
                viewModel.select(device)
            }
        }
        item {
            Text("Outputs", style = MaterialTheme.typography.titleMedium)
        }
        items(state.outputs, key = { it.id }) { device ->
            DeviceRow(device) { }
        }
        item {
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { viewModel.clear() }) { Text("Reset to system default") }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: AudioDeviceItem, onSelect: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    device.typeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (device.isCommunicationCandidate) {
                TextButton(onClick = onSelect) { Text("Use for calls") }
            }
        }
    }
}
