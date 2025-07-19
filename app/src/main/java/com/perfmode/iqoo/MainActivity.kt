package com.perfmode.iqoo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.perfmode.iqoo.AppContent
import com.perfmode.iqoo.ui.theme.AboutDialog
import com.perfmode.iqoo.ui.theme.InfoScreen
import com.perfmode.iqoo.ui.theme.IqooTweaksTheme
import com.perfmode.iqoo.ui.theme.SettingsScreen
import com.perfmode.iqoo.ui.theme.OverlaySelectionScreen
import com.perfmode.iqoo.util.DataStoreManager
import com.perfmode.iqoo.util.FeatureRepository
import com.perfmode.iqoo.util.ShellUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import rikka.shizuku.Shizuku
import android.util.Log
import androidx.activity.compose.BackHandler

// Define an enum to manage the current screen state
enum class AppScreen {
    MAIN,
    INFO,
    SETTINGS,
    OVERLAY_SELECTION
}

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    // ActivityResultLauncher for requesting POST_NOTIFICATIONS permission
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notification permission denied. App may not show background service notifications.", Toast.LENGTH_LONG).show()
        }
    }

    // ActivityResultLauncher for requesting SYSTEM_ALERT_WINDOW permission
    private val requestOverlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Overlay permission denied. Overlay may not work.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ActivityResultLauncher for requesting IGNORE_BATTERY_OPTIMIZATIONS
    private val requestBatteryOptimizationExemptionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Toast.makeText(this, "✅ Battery optimization ignored.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "❌ Battery optimization not ignored. Service may be killed.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Shizuku permission listener
    private val shizukuPermissionListener = object : Shizuku.OnRequestPermissionResultListener {
        override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
            if (requestCode == 100) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this@MainActivity, "✅ Shizuku permission granted\nMauj Kardi BETE!!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "❌ Shizuku permission denied\nFeatures may not work.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Shizuku binder dead listener
    private val shizukuBinderDeadListener = Shizuku.OnBinderDeadListener {
        Toast.makeText(this, "⚠️ Shizuku service disconnected. Please restart Shizuku Manager.", Toast.LENGTH_LONG).show()
        val serviceIntent = Intent(this@MainActivity, FeatureControlService::class.java).apply {
            action = FeatureControlService.ACTION_STOP_LOOP
        }
        startService(serviceIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity created.")

        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        Shizuku.addBinderDeadListener(shizukuBinderDeadListener)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please grant 'Display over other apps' permission for the overlay.", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                requestOverlayPermissionLauncher.launch(intent)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Toast.makeText(this, "Please allow ignoring battery optimizations for continuous service.", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                requestBatteryOptimizationExemptionLauncher.launch(intent)
            }
        }

        setContent {
            val context = LocalContext.current
            Log.d(TAG, "setContent: Composable content set.")

            var showAboutDialog by remember { mutableStateOf(false) }
            var currentScreen by remember { mutableStateOf(AppScreen.MAIN) }

            // BackHandler for system back button
            BackHandler(enabled = currentScreen == AppScreen.INFO || currentScreen == AppScreen.SETTINGS || currentScreen == AppScreen.OVERLAY_SELECTION) {
                Log.d(TAG, "BackHandler: System back button pressed. Navigating to MAIN.")
                currentScreen = AppScreen.MAIN
            }

            LaunchedEffect(Unit) {
                Log.d(TAG, "LaunchedEffect: Checking Shizuku status.")
                if (Shizuku.pingBinder()) {
                    if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(context, "Requesting Shizuku permission...", Toast.LENGTH_SHORT).show()
                        Shizuku.requestPermission(100)
                    } else {
                        Toast.makeText(context, "✅ Shizuku permission granted\nMauj Kardi BETE!!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "❌ Shizuku not available\nAiynnnnn", Toast.LENGTH_LONG).show()
                }
            }

            IqooTweaksTheme {
                SideEffect {
                    Log.d(TAG, "Current screen state: $currentScreen")
                }

                if (showAboutDialog) {
                    AboutDialog(onDismissRequest = { showAboutDialog = false })
                }

                when (currentScreen) {
                    AppScreen.MAIN -> {
                        AppContent(
                            context = this,
                            onStopClicked = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    val toggles = DataStoreManager.getFeatureTogglesFlow(this@MainActivity).first()
                                    // allFeatures is now collected in AppContent, so pass it from there
                                    val allFeaturesForStop = FeatureRepository.getAllFeaturesFlow(this@MainActivity).first()

                                    val resetCommands = allFeaturesForStop
                                        .filter { toggles[it.title] == true }
                                        .mapNotNull { it.resetCommand }
                                        .distinct()

                                    withContext(Dispatchers.Main) {
                                        if (resetCommands.isNotEmpty()) {
                                            Toast.makeText(this@MainActivity, "🔄 Resetting active features...", Toast.LENGTH_SHORT).show()
                                            resetCommands.forEach { command ->
                                                ShellUtils.runShellCommand(command)
                                            }
                                        }
                                        val serviceIntent = Intent(this@MainActivity, FeatureControlService::class.java).apply {
                                            action = FeatureControlService.ACTION_STOP_LOOP
                                        }
                                        startService(serviceIntent)
                                        DataStoreManager.clearActiveLoopCommands(this@MainActivity)
                                        Toast.makeText(this@MainActivity, "🛑 All active features disabled.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onAboutClicked = {
                                showAboutDialog = true
                            },
                            onInfoClicked = {
                                currentScreen = AppScreen.INFO
                            }
                        )
                    }
                    AppScreen.INFO -> {
                        InfoScreen(
                            features = FeatureRepository.getAllFeaturesFlow(LocalContext.current).collectAsState(initial = emptyList()).value,
                            onBackClicked = {
                                currentScreen = AppScreen.MAIN
                            }
                        )
                    }
                    AppScreen.SETTINGS -> {
                        SettingsScreen(
                            features = FeatureRepository.getAllFeaturesFlow(LocalContext.current).collectAsState(initial = emptyList()).value
                        )
                    }
                    AppScreen.OVERLAY_SELECTION -> {
                        OverlaySelectionScreen(
                            features = FeatureRepository.getAllFeaturesFlow(LocalContext.current).collectAsState(initial = emptyList()).value,
                            onBackClicked = {
                                currentScreen = AppScreen.MAIN
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        Shizuku.removeBinderDeadListener(shizukuBinderDeadListener)
    }
}