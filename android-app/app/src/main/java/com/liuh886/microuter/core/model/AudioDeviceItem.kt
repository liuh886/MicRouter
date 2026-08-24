package com.liuh886.microuter.core.model

import android.media.AudioDeviceInfo

data class AudioDeviceItem(
    val id: Int,
    val name: String,
    val typeLabel: String,
    val type: Int,
    val address: String,
    val isSource: Boolean,
    val isSink: Boolean,
    val isCommunicationCandidate: Boolean
)

val Int.audioTypeLabel: String
    get() = when (this) {
        AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Built-in microphone"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in speaker"
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headset"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headphones"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB audio device"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB headset"
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB accessory"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth stereo (A2DP)"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth headset (SCO/HFP)"
        AudioDeviceInfo.TYPE_BLE_HEADSET -> "Bluetooth LE headset"
        AudioDeviceInfo.TYPE_BLE_SPEAKER -> "Bluetooth LE speaker"
        AudioDeviceInfo.TYPE_TELEPHONY -> "Telephony"
        AudioDeviceInfo.TYPE_FM_TUNER -> "FM tuner"
        AudioDeviceInfo.TYPE_HDMI -> "HDMI"
        AudioDeviceInfo.TYPE_DOCK -> "Dock"
        else -> "Type $this"
    }

fun AudioDeviceInfo.toItem(isCommunicationCandidate: Boolean = false): AudioDeviceItem {
    val rawName = productName?.toString().orEmpty()
    return AudioDeviceItem(
        id = id,
        name = rawName.ifBlank { type.audioTypeLabel },
        typeLabel = type.audioTypeLabel,
        type = type,
        address = address.orEmpty(),
        isSource = isSource,
        isSink = isSink,
        isCommunicationCandidate = isCommunicationCandidate
    )
}
