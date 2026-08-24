package com.liuh886.microuter.core.model

import android.media.AudioDeviceInfo
import android.os.Build

data class AudioDeviceItem(
    val id: Int,
    val name: String,
    val typeLabel: String,
    val type: Int,
    val address: String,
    val isSource: Boolean,
    val isSink: Boolean,
    val isCommunicationCandidate: Boolean,
    val recordable: Boolean
)

val Int.isRecordableInputType: Boolean
    get() = when (this) {
        AudioDeviceInfo.TYPE_BUILTIN_MIC,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_ACCESSORY,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET -> true
        else -> false
    }

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
        AudioDeviceInfo.TYPE_REMOTE_SUBMIX -> "Remote submix"
        AudioDeviceInfo.TYPE_HDMI -> "HDMI"
        AudioDeviceInfo.TYPE_DOCK -> "Dock"
        else -> "Type $this"
    }

fun AudioDeviceInfo.toItem(isCommunicationCandidate: Boolean = false): AudioDeviceItem {
    val rawName = productName?.toString().orEmpty()
    val isModelName = rawName.isBlank() || rawName.equals(Build.MODEL, ignoreCase = true)
    val base = if (isModelName) type.audioTypeLabel else rawName
    val location = address.orEmpty()
    val name = if (isModelName && location.isNotBlank() && location != "0") {
        "$base ($location)"
    } else {
        base
    }
    return AudioDeviceItem(
        id = id,
        name = name,
        typeLabel = type.audioTypeLabel,
        type = type,
        address = location,
        isSource = isSource,
        isSink = isSink,
        isCommunicationCandidate = isCommunicationCandidate,
        recordable = isSource && type.isRecordableInputType
    )
}
