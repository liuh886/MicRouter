package com.liuh886.microuter.ui.mictest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liuh886.microuter.audio.ClipPlayer
import com.liuh886.microuter.audio.MicTester
import com.liuh886.microuter.core.model.AudioDeviceItem
import com.liuh886.microuter.core.model.RecordedClip
import com.liuh886.microuter.data.AudioRepository
import com.liuh886.microuter.ui.components.CapsuleButton
import com.liuh886.microuter.ui.components.Chevron
import com.liuh886.microuter.ui.components.CompareBar
import com.liuh886.microuter.ui.components.DeviceGlyph
import com.liuh886.microuter.ui.components.DevicePickerSheet
import com.liuh886.microuter.ui.components.GroupHeader
import com.liuh886.microuter.ui.components.LevelTrack
import com.liuh886.microuter.ui.components.ListRow
import com.liuh886.microuter.ui.components.PillAction
import com.liuh886.microuter.ui.components.PulsingDot
import com.liuh886.microuter.ui.components.SlotBadge
import com.liuh886.microuter.ui.theme.GlassCard
import com.liuh886.microuter.ui.theme.Hairline

@Composable
fun MicTestScreen(
    repository: AudioRepository,
    tester: MicTester,
    clipPlayer: ClipPlayer,
    micPermissionGranted: Boolean,
    onRequestMicPermission: () -> Unit
) {
    val viewModel: MicTestViewModel = viewModel { MicTestViewModel(repository, tester, clipPlayer) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selected = state.selected
    var showInputSheet by remember { mutableStateOf(false) }
    var showOutputSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Monitor",
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
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ACTUAL ROUTE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        proofText(state),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.sessionInfo?.linkConfirmed == false) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
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
                title = "Ear Monitor",
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
                title = "Listen Output",
                subtitle = state.monitorName?.let { "Manual: $it" } ?: "Auto (BT/wired first)",
                showDivider = true,
                onClick = { showOutputSheet = true }
            ) {
                Chevron()
            }
            ListRow(
                title = "System Route",
                subtitle = "Mode: ${state.modeLabel} · BT mic link ${if (state.btMicLinkUp) "UP" else "down"}",
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
        GroupHeader("A/B Compare — same phrase, two mics")
        GlassCard(cornerRadius = 20.dp) {
            SlotSection(
                slot = 'A',
                clip = state.clipA,
                capturing = state.captureSlot == 'A',
                playing = state.playingSlot == 'A',
                captureSeconds = state.captureSeconds,
                currentInput = selected,
                showDivider = true,
                maxRms = maxOf(state.clipA?.rms ?: 0f, state.clipB?.rms ?: 0f),
                onToggle = { viewModel.toggleCapture('A') },
                onPlay = { viewModel.playSlot('A') }
            )
            SlotSection(
                slot = 'B',
                clip = state.clipB,
                capturing = state.captureSlot == 'B',
                playing = state.playingSlot == 'B',
                captureSeconds = state.captureSeconds,
                currentInput = selected,
                showDivider = false,
                maxRms = maxOf(state.clipA?.rms ?: 0f, state.clipB?.rms ?: 0f),
                onToggle = { viewModel.toggleCapture('B') },
                onPlay = { viewModel.playSlot('B') }
            )
            if (state.clipA != null && state.clipB != null) {
                Text(
                    "Bars normalized to the louder clip",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, bottom = 10.dp)
                )
            }
        }
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
    }
    if (showInputSheet) {
        DevicePickerSheet(
            title = "Choose Input — switches live",
            devices = state.inputs.filter { it.isSource },
            selectedId = selected?.id,
            allowPick = { it.recordable },
            disabledTag = "not recordable",
            allowedTag = "test input",
            onPick = { device ->
                viewModel.selectDevice(device)
                showInputSheet = false
            },
            onDismiss = { showInputSheet = false }
        )
    }
    if (showOutputSheet) {
        DevicePickerSheet(
            title = "Listen Output — where you hear yourself",
            devices = state.outputs,
            selectedId = state.monitorOutputId,
            allowPick = { viewModel.isMonitorCapable(it) },
            disabledTag = "not monitorable",
            allowedTag = "monitor out",
            onPick = { device ->
                viewModel.selectMonitorOutput(device)
                showOutputSheet = false
            },
            onDismiss = { showOutputSheet = false }
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

private fun proofText(state: MicTestViewModel.UiState): String {
    val info = state.sessionInfo ?: return "—"
    var text = info.routedDeviceName ?: "unknown"
    if (info.linkConfirmed == false) text += "  ⚠ SCO unconfirmed"
    return text
}

@Composable
private fun SlotSection(
    slot: Char,
    clip: RecordedClip?,
    capturing: Boolean,
    playing: Boolean,
    captureSeconds: Int,
    currentInput: AudioDeviceItem?,
    showDivider: Boolean,
    maxRms: Float,
    onToggle: () -> Unit,
    onPlay: () -> Unit
) {
    Column(Modifier.padding(top = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SlotBadge(slot)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    clip?.deviceName ?: currentInput?.name ?: "Input $slot",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    clip?.let { "${it.sampleRate} Hz · ${it.durationMs / 1000}s · peak ${it.peak}" }
                        ?: if (capturing) "Recording… ${captureSeconds}s" else "Not recorded yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PillAction(
                text = if (capturing) "Stop" else "Rec",
                onClick = onToggle
            )
        }
        if (clip != null) {
            Spacer(Modifier.height(8.dp))
            CompareBar(
                fraction = if (maxRms > 0f) clip.rms / maxRms else 0f,
                accent = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "RMS ${(clip.rms * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PillAction(
                    text = if (playing) "Stop" else "Play",
                    onClick = onPlay
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                thickness = Hairline,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            )
        }
    }
}
