package com.perfmode.iqoo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.perfmode.iqoo.util.DataStoreManager
import com.perfmode.iqoo.util.ShellUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FeatureControlService : Service() {

    private val TAG = "FeatureControlService"
    private lateinit var serviceScope: CoroutineScope

    private val NOTIFICATION_CHANNEL_ID = "FeatureControlChannel"
    private val NOTIFICATION_ID = 100 // Different ID from OverlayControlService

    companion object {
        const val ACTION_START_LOOP = "com.perfmode.iqoo.ACTION_START_LOOP"
        const val ACTION_STOP_LOOP = "com.perfmode.iqoo.ACTION_STOP_LOOP"
        const val EXTRA_COMMANDS = "com.perfmode.iqoo.EXTRA_COMMANDS"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service: onCreate called.")
        serviceScope = CoroutineScope(Dispatchers.Main)

        // Notification channel creation should typically happen once per app lifecycle,
        // often in Application.onCreate() or MainActivity.onCreate()
        // Calling it here is okay as createNotificationChannel is idempotent,
        // but the crash log points to an issue with deleting a channel already in use.
        // This suggests an external deletion or a very specific device behavior.
        // We will move startForeground to onStartCommand for better control.

        // Attempt to resume previous loop if service restarted
        serviceScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Service: Checking for saved loop commands to resume (from onCreate)...")
            val savedCommands = DataStoreManager.getActiveLoopCommands(applicationContext)
            if (savedCommands.isNotEmpty()) {
                Log.d(TAG, "Service: Resuming saved loop commands: $savedCommands")
                // Note: startForeground needs to be called in onStartCommand,
                // so this resume might not directly restart the FGS without onStartCommand.
                // We'll rely on onStartCommand being called if the system explicitly restarts the FGS.
            } else {
                Log.d(TAG, "Service: No saved loop commands found to resume.")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service: onStartCommand called with action: ${intent?.action}")

        // --- Foreground Service Setup (Moved here for explicit control) ---
        // Ensure notification channel exists (idempotent)
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("iQOO Tweaks Features")
            .setContentText("Features are active in background.")
            .setSmallIcon(R.mipmap.ic_launcher_round) // Use your app's launcher icon
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

        // Call startForeground ONLY if not already in foreground state to avoid crashes/warnings
        // (though startForeground is safe to call multiple times with the same ID)
        try {
            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "Service: Called startForeground with ID $NOTIFICATION_ID.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call startForeground: ${e.message}", e)
            // If startForeground fails, it implies a serious issue, stop self.
            Toast.makeText(this, "Failed to start background service.", Toast.LENGTH_LONG).show()
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_START_LOOP -> {
                val commands = intent?.getStringArrayListExtra(EXTRA_COMMANDS)
                if (!commands.isNullOrEmpty()) {
                    Log.d(TAG, "Service: Received ACTION_START_LOOP. Commands: $commands")
                    serviceScope.launch(Dispatchers.IO) { // Launch on IO dispatcher for shell ops
                        Log.d(TAG, "Service: Launching ShellUtils.runCommandsLoop for: $commands")
                        ShellUtils.runCommandsLoop(commands)
                        Log.d(TAG, "Service: Saving active loop commands to DataStore: $commands")
                        DataStoreManager.saveActiveLoopCommands(applicationContext, commands)
                    }
                } else {
                    Log.w(TAG, "Service: ACTION_START_LOOP received but commands list is empty or null.")
                    ShellUtils.stopExecution()
                    serviceScope.launch(Dispatchers.IO) { DataStoreManager.clearActiveLoopCommands(applicationContext) }
                }
            }
            ACTION_STOP_LOOP -> {
                Log.d(TAG, "Service: Received ACTION_STOP_LOOP. Stopping current loop.")
                ShellUtils.stopExecution()
                serviceScope.launch(Dispatchers.IO) {
                    Log.d(TAG, "Service: Clearing active loop commands from DataStore.")
                    DataStoreManager.clearActiveLoopCommands(applicationContext)
                }
                stopSelf() // Stop the service itself
                Log.d(TAG, "Service: Called stopSelf().")
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service: onDestroy called. Ensuring loop is stopped and scope cancelled.")
        ShellUtils.stopExecution() // Ensure loop is stopped
        // FIX: stopForeground(true) should be called to remove the notification and stop the service
        // However, the crash is "Not allowed to delete channel with FGS".
        // This implies stopSelf() or stopForeground(true) is already implicitly handled,
        // and the channel is being deleted elsewhere or recreated in a conflicting way.
        stopForeground(true) // Call stopForeground to remove notification
        serviceScope.cancel() // Cancel all coroutines
        Log.d(TAG, "Service: onDestroy completed.")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Feature Control Channel",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Channel for background feature control service"
            }
            val manager = getSystemService(NotificationManager::class.java)
            // This method is idempotent; it creates the channel if it doesn't exist.
            // It does NOT delete existing channels unless channel properties change AND system permits.
            manager.createNotificationChannel(serviceChannel)
            Log.d(TAG, "Notification channel '$NOTIFICATION_CHANNEL_ID' created (if not already exists).")
        }
    }
}