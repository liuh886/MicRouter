package com.liuh886.microuter.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

class CommunicationController(context: Context) {

    private val audioManager = context.getSystemService(AudioManager::class.java)

    fun current(): AudioDeviceInfo? = audioManager.communicationDevice

    fun candidates(): List<AudioDeviceInfo> = audioManager.availableCommunicationDevices

    fun select(device: AudioDeviceInfo): Boolean = audioManager.setCommunicationDevice(device)

    fun clear() {
        audioManager.clearCommunicationDevice()
    }
}
