package com.perfmode.iqoo.util

import androidx.compose.ui.graphics.vector.ImageVector
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import android.util.Log // For logging potential issues

/**
 * Custom Gson TypeAdapter for ImageVector.
 * It serializes ImageVector to a String (its name) and deserializes it back.
 * This is crucial because ImageVector cannot be directly serialized by Gson.
 */
class ImageVectorAdapter : TypeAdapter<ImageVector>() {

    private val TAG = "ImageVectorAdapter"

    override fun write(out: JsonWriter, value: ImageVector?) {
        if (value == null) {
            out.nullValue()
            return
        }
        // When writing, we just write the ImageVector's name as a string.
        // This requires a consistent way to get the name (e.g., from Icons.Default.XYZ)
        val iconName = when (value) {
            Icons.Default.Apps -> "Apps"
            Icons.Default.Memory -> "Memory"
            Icons.Default.Terminal -> "Terminal"
            Icons.Default.DeveloperMode -> "Processor" // Map back to "Processor" as used in Features
            Icons.Default.Speed -> "Speed"
            Icons.Default.TouchApp -> "TouchApp"
            Icons.Default.Whatshot -> "Whatshot"
            Icons.Default.Refresh -> "Refresh"
            Icons.Default.ScreenRotation -> "ScreenRotation"
            Icons.Default.MovieFilter -> "MovieFilter"
            Icons.Default.HighQuality -> "HighQuality"
            Icons.Default.BrightnessHigh -> "BrightnessHigh"
            Icons.Default.LockOpen -> "LockOpen"
            Icons.Default.Sync -> "Sync"
            Icons.Default.BatteryChargingFull -> "BatteryChargingFull"
            Icons.Default.WbSunny -> "WbSunny"
            Icons.Default.Rocket -> "Rocket"
            Icons.Default.Games -> "Games"
            Icons.Default.DisplaySettings -> "DisplaySettings"
            Icons.Default.Tune -> "Tune"
            Icons.Default.Thermostat -> "Thermostat"
            Icons.Default.Animation -> "Animation"
            Icons.Default.SportsEsports -> "SportsEsports"
            Icons.Default.Loop -> "Loop"
            Icons.Default.Gesture -> "Gesture"
            Icons.Default.Extension -> "Extension" // Fallback icon name
            // Add all other Icons.Default you use in your predefined features here
            else -> {
                Log.w(TAG, "Unknown ImageVector encountered during serialization: $value. Saving as 'Extension'.")
                "Extension" // Fallback for any unknown ImageVectors
            }
        }
        out.value(iconName)
    }

    override fun read(reader: JsonReader): ImageVector? {
        if (reader.peek() == com.google.gson.stream.JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        // When reading, we read the string name and map it back to an ImageVector.
        val iconName = reader.nextString()
        return when (iconName) {
            "Apps" -> Icons.Default.Apps
            "Memory" -> Icons.Default.Memory
            "Terminal" -> Icons.Default.Terminal
            "Processor" -> Icons.Default.DeveloperMode
            "Speed" -> Icons.Default.Speed
            "TouchApp" -> Icons.Default.TouchApp
            "Whatshot" -> Icons.Default.Whatshot
            "Refresh" -> Icons.Default.Refresh
            "ScreenRotation" -> Icons.Default.ScreenRotation
            "MovieFilter" -> Icons.Default.MovieFilter
            "HighQuality" -> Icons.Default.HighQuality
            "BrightnessHigh" -> Icons.Default.BrightnessHigh
            "LockOpen" -> Icons.Default.LockOpen
            "Sync" -> Icons.Default.Sync
            "BatteryChargingFull" -> Icons.Default.BatteryChargingFull
            "WbSunny" -> Icons.Default.WbSunny
            "Rocket" -> Icons.Default.Rocket
            "Games" -> Icons.Default.Games
            "DisplaySettings" -> Icons.Default.DisplaySettings
            "Tune" -> Icons.Default.Tune
            "Thermostat" -> Icons.Default.Thermostat
            "Animation" -> Icons.Default.Animation
            "SportsEsports" -> Icons.Default.SportsEsports
            "Loop" -> Icons.Default.Loop
            "Gesture" -> Icons.Default.Gesture
            // Add all other Icons.Default you use here
            else -> {
                Log.w(TAG, "Unknown iconName '$iconName' encountered during deserialization. Using 'Extension'.")
                Icons.Default.Extension // Fallback for unknown icon names
            }
        }
    }
}