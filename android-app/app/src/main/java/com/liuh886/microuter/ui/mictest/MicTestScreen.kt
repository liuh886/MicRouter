package com.liuh886.microuter.ui.mictest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liuh886.microuter.audio.MicTester
import com.liuh886.microuter.data.AudioRepository
import com.liuh886.microuter.ui.components.CapsuleButton
import com.liuh886.microuter.ui.components.CheckTrailing
import com.liuh886.microuter.ui.components.DeviceGlyph
import com.liuh886.microuter.ui.components.DevicePickerSheet
import com.liuh886.microuter.ui.components.GroupHeader
import com.liuh886.microuter.ui.components.LevelTrack
import com.liuh886.microuter.ui.components.ListRow
import com.liuh886.microuter.ui.components.PillAction
import com.liuh886.microuter.ui.components.PulsingDot
import com.liuh886.microuter.ui.theme.GlassCard

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
    var showInputSheet by remember { mutableStateOf(false) }

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
        AnimatedVisibility(visible = state.error != null) {
            state.error?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        GlassCard(cornerRadius = 24.dp) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "LEVEL",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${(state.level * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(10.dp))
                LevelTrack(if (state.running) state.level else 0f)
            }
            ListRow(
                title = selected?.name ?: "Choose input device",
                subtitle = selected?.typeLabel ?: "Tap to open the picker",
                leading = { selected?.let { d -> DeviceGlyph(d.type) } },
                showDivider = true,
                onClick = { showInputSheet = true }
            ) {
                if (state.running) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PulsingDot()
                        Spacer(Modifier.padding(start = 5.dp))
                        Text(
                            "Live",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ListRow(
                title = "Ear Monitor 耳返",
                subtitle = if (state.earMonitor) {
                    "Via ${state.monitorName ?: "output"} · BT delay 100-300ms is normal"
                } else {
                    "Hear your microphone in real time"
                },
                showDivider = true
            ) {
                Switch(
                    checked = state.earMonitor,
                    onCheckedChange = { viewModel.setEarMonitor(it) }
                )
            }
            ListRow(
                title = "System Route",
                subtitle = "Mode: ${state.modeLabel}",
                showDivider = false
            ) {
                Text(
                    state.systemComm?.name ?: "Auto",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        CapsuleButton(
            text = if (state.running) "Stop" else "Start",
            enabled = micPermissionGranted
        ) { viewModel.toggleRun() }
        GlassCard(cornerRadius = 16.dp) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    "SESSION",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    sessionSubtitle(state),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        GroupHeader("Outputs — call route")
        GlassCard(cornerRadius = 20.dp) {
            state.outputs.forEachIndexed { index, device ->
                val isActive = state.systemComm?.id == device.id
                ListRow(
                    title = device.name,
                    subtitle = device.typeLabel,
                    leading = { DeviceGlyph(device.type) },
                    showDivider = index < state.outputs.lastIndex,
                    onClick = if (device.isCommunicationCandidate) {
                        { viewModel.selectOutput(device) }
                    } else {
                        null
                    }
                ) {
                    if (isActive) {
                        CheckTrailing()
                    } else if (device.isCommunicationCandidate) {
                        PillAction("Use") { viewModel.selectOutput(device) }
                    }
                }
            }
        }
    }
    if (showInputSheet) {
        DevicePickerSheet(
            title = "Choose Input — switches live",
            devices = state.inputs,
            selectedId = selected?.id,
            onPick = { device ->
                viewModel.selectDevice(device)
                showInputSheet = false
            },
            onDismiss = { showInputSheet = false }
        )
    }
}

private fun sessionSubtitle(state: MicTestViewModel.UiState): String {
    val info = state.sessionInfo ?: return "Idle — press Start, then switch devices live"
    val text = StringBuilder("${info.sampleRate} Hz · ${info.sourceLabel}")
    info.routedDeviceName?.let { text.append(" · actual→").append(it) }
    if (info.linkConfirmed == false) text.append(" · ⚠ SCO link unconfirmed")
    info.monitorOutputName?.let { text.append(" · ear→").append(it) }
    return text.toString()
}
