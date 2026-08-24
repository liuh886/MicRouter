package com.liuh886.microuter.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.liuh886.microuter.core.model.AudioDeviceItem
import com.liuh886.microuter.ui.theme.AppPalette
import com.liuh886.microuter.ui.theme.Hairline
import android.media.AudioDeviceInfo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dock
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SettingsInputHdmi
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VolumeUp

@Composable
fun GroupHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, bottom = 6.dp)
    )
}

@Composable
fun ListRow(
    title: String,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val rowModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Row(
        modifier = rowModifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(start = if (leading != null) 60.dp else 16.dp),
            thickness = Hairline,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun PillAction(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun CheckTrailing() {
    Icon(
        Icons.Filled.Check,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(22.dp)
    )
}

@Composable
fun ValueTrailing(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
}

@Composable
fun StatusPill(text: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun CapsuleButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val container = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(container)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White.copy(alpha = if (enabled) 1f else 0.7f)
        )
    }
}

@Composable
fun LevelTrack(level: Float) {
    val animated by animateFloatAsState(targetValue = level.coerceIn(0f, 1f), label = "level")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(10.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
        )
    }
}

private data class Glyph(val icon: ImageVector, val accent: Color)

@Composable
private fun accentPrimary(): Color = MaterialTheme.colorScheme.primary

@Composable
private fun glyphFor(type: Int): Glyph {
    val dark = isSystemInDarkTheme()
    val red = if (dark) AppPalette.RedDark else AppPalette.RedLight
    val purple = if (dark) AppPalette.PurpleDark else AppPalette.PurpleLight
    val orange = if (dark) AppPalette.OrangeDark else AppPalette.OrangeLight
    val indigo = if (dark) AppPalette.IndigoDark else AppPalette.IndigoLight
    return when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_MIC -> Glyph(Icons.Filled.Mic, red)
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> Glyph(Icons.Filled.VolumeUp, accentPrimary())
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> Glyph(Icons.Filled.Phone, accentPrimary())
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> Glyph(Icons.Filled.Headphones, orange)
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> Glyph(Icons.Filled.Headset, orange)
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> Glyph(Icons.Filled.Usb, purple)
        AudioDeviceInfo.TYPE_USB_HEADSET -> Glyph(Icons.Filled.HeadsetMic, purple)
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> Glyph(Icons.Filled.BluetoothAudio, indigo)
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET -> Glyph(Icons.Filled.HeadsetMic, indigo)
        AudioDeviceInfo.TYPE_BLE_SPEAKER -> Glyph(Icons.Filled.Bluetooth, indigo)
        AudioDeviceInfo.TYPE_FM_TUNER -> Glyph(Icons.Filled.Radio, orange)
        AudioDeviceInfo.TYPE_HDMI -> Glyph(Icons.Filled.SettingsInputHdmi, orange)
        AudioDeviceInfo.TYPE_DOCK -> Glyph(Icons.Filled.Dock, orange)
        AudioDeviceInfo.TYPE_TELEPHONY -> Glyph(Icons.Filled.Phone, accentPrimary())
        else -> Glyph(Icons.Filled.GraphicEq, accentPrimary())
    }
}

@Composable
fun DeviceGlyph(type: Int) {
    val glyph = glyphFor(type)
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(glyph.accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(glyph.icon, contentDescription = null, tint = glyph.accent, modifier = Modifier.size(19.dp))
    }
}

@Composable
fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PulsingDot(color: Color = MaterialTheme.colorScheme.primary) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    Box(Modifier.size(8.dp).background(color.copy(alpha = alpha), CircleShape))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePickerSheet(
    title: String,
    devices: List<AudioDeviceItem>,
    selectedId: Int?,
    onPick: (AudioDeviceItem) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
            devices.forEach { device ->
                ListRow(
                    title = device.name,
                    subtitle = device.typeLabel,
                    leading = { DeviceGlyph(device.type) },
                    showDivider = device !== devices.last(),
                    onClick = { onPick(device) }
                ) {
                    if (device.id == selectedId) {
                        CheckTrailing()
                    } else if (device.isCommunicationCandidate) {
                        Text(
                            "controllable",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}
