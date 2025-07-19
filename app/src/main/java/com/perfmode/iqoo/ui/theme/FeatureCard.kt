package com.perfmode.iqoo.ui.theme

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perfmode.iqoo.model.Feature
import com.perfmode.iqoo.util.DataStoreManager
import com.perfmode.iqoo.util.ShellUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.perfmode.iqoo.OverlayControlService
import android.util.Log
import android.provider.Settings
import android.net.Uri
import android.os.Build


@Composable
fun FeatureCard(
    context: Context,
    feature: Feature,
    isEnabled: Boolean,
    onToggleChanged: (Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val localContext = LocalContext.current
    val TAG = "FeatureCard"

    val isChecked = remember { mutableStateOf(isEnabled) }

    LaunchedEffect(feature.title) {
        DataStoreManager.getFeatureTogglesFlow(context).collect { toggles ->
            val dataStoreValue = toggles[feature.title] ?: false
            if (isChecked.value != dataStoreValue) {
                Log.d(TAG, "LaunchedEffect for ${feature.title}: DataStore value changed from ${isChecked.value} to $dataStoreValue. Updating UI.")
                isChecked.value = dataStoreValue
            } else {
                Log.d(TAG, "LaunchedEffect for ${feature.title}: DataStore value $dataStoreValue matches UI state.")
            }

            val serviceIntent = Intent(context, OverlayControlService::class.java).apply {
                action = OverlayControlService.ACTION_UPDATE_OVERLAY_STATE
                putExtra(OverlayControlService.EXTRA_TOGGLE_STATE_MAP, HashMap(toggles))
            }
            try {
                context.startService(serviceIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send overlay update intent: ${e.message}")
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = feature.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = feature.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
            Switch(
                checked = isChecked.value,
                onCheckedChange = { newCheckedState ->
                    Log.d(TAG, "${feature.title}: Switch clicked. Old state: ${isChecked.value}, New state: $newCheckedState")
                    isChecked.value = newCheckedState

                    coroutineScope.launch {
                        Log.d(TAG, "${feature.title}: Saving state $newCheckedState to DataStore.")
                        DataStoreManager.setFeatureEnabled(context, feature.title, newCheckedState)

                        if (!newCheckedState) {
                            feature.resetCommand?.let { resetCmd ->
                                Log.d(TAG, "${feature.title}: Toggle OFF, executing reset command: $resetCmd")
                                ShellUtils.runShellCommand(resetCmd)
                                Toast.makeText(localContext, "${feature.title} reset.", Toast.LENGTH_SHORT).show()
                            } ?: run {
                                Log.d(TAG, "${feature.title}: Toggle OFF, no reset command defined.")
                            }
                        } else {
                            if (!feature.canLoopAsSpecialCase) {
                                Log.d(TAG, "${feature.title}: Toggle ON, executing run-once command: ${feature.command}")
                                ShellUtils.runShellCommand(feature.command)
                                Toast.makeText(localContext, "${feature.title} activated.", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.d(TAG, "${feature.title}: Toggle ON, is a special loop case. MainScreen's LaunchedEffect will handle looping via FeatureControlService.")
                            }
                        }
                    }
                    onToggleChanged(newCheckedState)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}