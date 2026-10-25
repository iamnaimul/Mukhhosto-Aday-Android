
package com.mukhhosto.aday

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.mukhhosto.aday.ui.MainScreen
import com.mukhhosto.aday.ui.MainViewModel
import com.mukhhosto.aday.ui.PrivacyDisclosureScreen
import com.mukhhosto.aday.ui.theme.MukhhostoTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private var requestingMicPermission = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            requestingMicPermission = false
            if (granted) {
                // Start immediately after the first-time permission grant.
                vm.startRecording()
            } else {
                vm.showPermissionDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                vm.onAppBackgrounded()
                finish()
            }
        })

        // Protect student names/transcripts from screenshots and recent-task previews.
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            MukhhostoTheme {
                // Consent is intentionally not persisted; there is no preferences/database storage.
                var acknowledged by remember { mutableStateOf(false) }
                if (!acknowledged) {
                    PrivacyDisclosureScreen(
                        onAccept = { acknowledged = true }
                    )
                } else {
                    MainScreen(
                        vm = vm,
                        onRequestMicPermission = {
                            if (ContextCompat.checkSelfPermission(
                                    this, Manifest.permission.RECORD_AUDIO
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                requestingMicPermission = true
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                false
                            } else true
                        }
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!requestingMicPermission) vm.onAppBackgrounded()
    }
}
