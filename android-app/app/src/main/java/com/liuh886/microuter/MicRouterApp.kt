package com.liuh886.microuter

import android.app.Application
import com.liuh886.microuter.audio.AudioDeviceScanner
import com.liuh886.microuter.audio.CommunicationController
import com.liuh886.microuter.audio.MicTester
import com.liuh886.microuter.audio.RouteMonitor
import com.liuh886.microuter.data.AudioRepository
import com.liuh886.microuter.data.RouteEventLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MicRouterApp : Application() {

    override fun onCreate() {
        super.onCreate()
        audioRepository.start()
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val scanner: AudioDeviceScanner by lazy { AudioDeviceScanner(this) }
    val communicationController: CommunicationController by lazy { CommunicationController(this) }
    val routeMonitor: RouteMonitor by lazy { RouteMonitor(this) }
    val eventLogger: RouteEventLogger by lazy { RouteEventLogger() }
    val micTester: MicTester by lazy {
        MicTester { device ->
            if (device != null) {
                audioRepository.beginLink(device)
            } else {
                audioRepository.endLink()
                true
            }
        }
    }
    val audioRepository: AudioRepository by lazy {
        AudioRepository(this, scanner, communicationController, routeMonitor, eventLogger, appScope)
    }
}
