package com.liuh886.microuter.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager

class RouteMonitor(
    context: Context,
    private val onChanged: (String) -> Unit
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
            onChanged("Added: ${addedDevices.joinToString { it.productName.toString() }}")
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
            onChanged("Removed: ${removedDevices.joinToString { it.productName.toString() }}")
        }
    }

    fun start() {
        audioManager.registerAudioDeviceCallback(callback, null)
    }

    fun stop() {
        audioManager.unregisterAudioDeviceCallback(callback)
    }
}
