package com.liuh886.microuter.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

class AudioDeviceScanner(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)

    fun inputDevices(): List<AudioDeviceInfo> {
        return audioManager
            .getDevices(AudioManager.GET_DEVICES_INPUTS)
            .toList()
    }

    fun communicationDevices(): List<AudioDeviceInfo> {
        return if (android.os.Build.VERSION.SDK_INT >= 31) {
            audioManager.availableCommunicationDevices.toList()
        } else {
            emptyList()
        }
    }

    fun describe(device: AudioDeviceInfo): String {
        return buildString {
            append("id=${device.id}\n")
            append("type=${device.type}\n")
            append("product=${device.productName}\n")
            append("address=${device.address}\n")
        }
    }
}
