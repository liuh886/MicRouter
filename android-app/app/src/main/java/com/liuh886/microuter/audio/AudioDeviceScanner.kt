package com.liuh886.microuter.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

class AudioDeviceScanner(context: Context) {

    private val audioManager = context.getSystemService(AudioManager::class.java)

    fun allDevices(): List<AudioDeviceInfo> =
        audioManager.getDevices(AudioManager.GET_DEVICES_ALL).toList()

    fun inputDevices(): List<AudioDeviceInfo> =
        audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).toList()

    fun outputDevices(): List<AudioDeviceInfo> =
        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()

    fun communicationCandidates(): List<AudioDeviceInfo> =
        audioManager.availableCommunicationDevices
}
