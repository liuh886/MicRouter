package com.liuh886.microuter.core.model

import android.media.AudioDeviceInfo
import android.media.AudioManager

data class AudioSessionState(
    val modeLabel: String = MODE_NORMAL_LABEL,
    val communicationDevice: AudioDeviceItem? = null,
    val inputs: List<AudioDeviceItem> = emptyList(),
    val outputs: List<AudioDeviceItem> = emptyList(),
    val communicationCandidates: List<AudioDeviceItem> = emptyList()
) {
    val isBluetoothCommunication: Boolean
        get() = communicationDevice?.let {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
        } == true

    companion object {
        const val MODE_NORMAL_LABEL = "NORMAL"
        const val MODE_RINGTONE_LABEL = "RINGTONE"
        const val MODE_IN_CALL_LABEL = "IN_CALL (telecom-managed)"
        const val MODE_IN_COMMUNICATION_LABEL = "IN_COMMUNICATION (VoIP)"
        const val MODE_UNKNOWN_LABEL = "UNKNOWN"

        fun labelForMode(mode: Int): String = when (mode) {
            AudioManager.MODE_NORMAL -> MODE_NORMAL_LABEL
            AudioManager.MODE_RINGTONE -> MODE_RINGTONE_LABEL
            AudioManager.MODE_IN_CALL -> MODE_IN_CALL_LABEL
            AudioManager.MODE_IN_COMMUNICATION -> MODE_IN_COMMUNICATION_LABEL
            else -> "$MODE_UNKNOWN_LABEL ($mode)"
        }
    }
}
