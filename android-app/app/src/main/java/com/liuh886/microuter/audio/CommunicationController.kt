package com.liuh886.microuter.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

class CommunicationController(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)

    fun current(): AudioDeviceInfo? {
        return if (android.os.Build.VERSION.SDK_INT >= 31) {
            audioManager.communicationDevice
        } else {
            null
        }
    }

    fun select(device: AudioDeviceInfo): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= 31) {
            audioManager.setCommunicationDevice(device)
        } else {
            false
        }
    }

    fun clear() {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            audioManager.clearCommunicationDevice()
        }
    }
}
