package com.fly.pkg

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.fly.pkg.service.ServiceState
import com.fly.pkg.ui.MainScreen
import com.fly.pkg.ui.theme.PkgFloatTheme

class MainActivity : ComponentActivity() {

    private val overlayGranted = mutableStateOf(false)
    private val a11yEnabled = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ForegroundPageRepository.setFloatEnabled(SettingsStore(this).floatEnabled)
        setContent {
            PkgFloatTheme {
                val floatEnabled by ForegroundPageRepository.floatEnabled.collectAsState()
                val serviceRunning by ServiceState.running.collectAsState()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        floatEnabled = floatEnabled,
                        overlayGranted = overlayGranted.value,
                        a11yEnabled = a11yEnabled.value,
                        serviceRunning = serviceRunning,
                        onToggleFloat = { enabled ->
                            SettingsStore(this).floatEnabled = enabled
                            ForegroundPageRepository.setFloatEnabled(enabled)
                        },
                        onOpenOverlaySettings = {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName"),
                                )
                            )
                        },
                        onOpenA11ySettings = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayGranted.value = Settings.canDrawOverlays(this)
        a11yEnabled.value = PermissionChecks.isAccessibilityServiceEnabled(this)
    }
}
