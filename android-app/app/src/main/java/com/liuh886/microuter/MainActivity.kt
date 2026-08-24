package com.liuh886.microuter

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.liuh886.microuter.ui.RootScaffold
import com.liuh886.microuter.ui.theme.MicRouterTheme

class MainActivity : ComponentActivity() {

    private var micPermissionGranted by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            refreshMicPermissionState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MicRouterApp
        app.audioRepository.start()
        requestNeededPermissions()
        setContent {
            MicRouterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RootScaffold(
                        app = app,
                        micPermissionGranted = micPermissionGranted,
                        onRequestMicPermission = { requestNeededPermissions() }
                    )
                }
            }
        }
    }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PERMISSION_GRANTED

    private fun refreshMicPermissionState() {
        micPermissionGranted = granted(Manifest.permission.RECORD_AUDIO)
    }

    private fun requestNeededPermissions() {
        val needed = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH_CONNECT
        ).filterNot { granted(it) }
        if (needed.isEmpty()) {
            refreshMicPermissionState()
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}
