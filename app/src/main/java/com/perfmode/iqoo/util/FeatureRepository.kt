package com.perfmode.iqoo.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.perfmode.iqoo.model.Feature
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import android.util.Log
import java.util.UUID


object FeatureRepository {

    // Pre-defined static features - Updated to use icon: ImageVector directly
    private val predefinedFeatures = listOf(
        Feature(
            id = UUID.randomUUID().toString(),
            title = "☛ MONSTER MODE PLUS ☚",
            command = "settings put system bench_mark_mode 1",
            category = "System",
            icon = Icons.Default.FlashOn, // FIX: Use ImageVector directly
            resetCommand = "settings put system bench_mark_mode 0",
            description = "Activates the highest performance mode, pushing CPU/GPU limits for maximum power. May increase heat and battery drain.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Monster Mode",
            command = "settings put system power_save_type 5",
            category = "System",
            icon = Icons.Default.Speed, // FIX: Use ImageVector directly
            resetCommand = "settings put system power_save_type -1",
            description = "Enables a high-performance profile, balancing power and efficiency for demanding tasks.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Normal Mode",
            command = "settings put system power_save_type -1",
            category = "System",
            icon = Icons.Default.Settings, // FIX: Use ImageVector directly
            resetCommand = null,
            description = "Reverts to the standard power-saving configuration, offering a balance of performance and battery life.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "APM Enhancement",
            command = "settings put global apm_enhancement_enabled 0",
            category = "System",
            icon = Icons.Default.BatteryChargingFull, // FIX: Use ImageVector directly
            resetCommand = "settings put global apm_enhancement_enabled 1",
            description = "Controls APM (Advanced Power Management) enhancements, which can affect power efficiency.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "High Brightness Mode",
            command = "settings put system vivo_hbm_level 3",
            category = "System",
            icon = Icons.Default.WbSunny, // FIX: Use ImageVector directly
            resetCommand = "settings put system vivo_hbm_level 1",
            description = "Activates a high brightness mode, increasing screen luminance beyond typical limits. CAUTION: experimental feature, needs more testing.",
            isCustom = false
        ),
        // Gaming Features
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Target FPS 144",
            command = "settings put system gamewatch_game_target_fps 144",
            category = "Gaming",
            icon = Icons.Default.Speed, // FIX: Use ImageVector directly
            canLoopAsSpecialCase = true,
            resetCommand = "settings put system gamewatch_game_target_fps -1",
            description = "Sets the target frame rate for games to 144 FPS, aiming for ultra-smooth gameplay.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Target FPS 120",
            command = "settings put system gamewatch_game_target_fps 120",
            category = "Gaming",
            icon = Icons.Default.Speed, // FIX: Use ImageVector directly
            canLoopAsSpecialCase = true,
            resetCommand = "settings put system gamewatch_game_target_fps -1",
            description = "Sets the target frame rate for games to 120 FPS, providing a high-refresh-rate experience.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Game Mode",
            command = "settings put system is_game_mode 1",
            category = "Gaming",
            icon = Icons.Default.SportsEsports, // FIX: Use ImageVector directly
            canLoopAsSpecialCase = true,
            resetCommand = "settings put system is_game_mode 0",
            description = "Enables the device's specialized game mode, which optimizes various system parameters for gaming performance.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Game Interpolation 90 FPS",
            command = "settings put system gamecube_frame_interpolation 1:3::48:90",
            category = "Gaming",
            icon = Icons.Default.Animation, // FIX: Use ImageVector directly
            canLoopAsSpecialCase = true,
            resetCommand = "settings put system gamecube_frame_interpolation 0:-1:0:0:0",
            description = "Enables frame interpolation to boost game frame rates to 90 FPS, creating a smoother visual experience.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Surface Flinger Boost",
            command = "setprop debug.sf.max_frames_ahead 1",
            category = "Gaming",
            icon = Icons.Default.SportsEsports, // FIX: Use ImageVector directly
            resetCommand = "setprop debug.sf.max_frames_ahead 2",
            description = "Reduce surface flinger buffering for faster gpu rendering speed improves gaming experience",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Disable Vsync",
            command = "setprop debug.egl.swapinterval 0",
            category = "Gaming",
            icon = Icons.Default.DisplaySettings, // FIX: Use ImageVector directly
            resetCommand = "setprop debug.egl.swapinterval 1",
            description = "Disables Display Vsync use this with caution might cause stuttering",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Disable GR Swap",
            command = "setprop debug.gr.swapinterval 0",
            category = "Gaming",
            icon = Icons.Default.DisplaySettings, // FIX: Use ImageVector directly
            resetCommand = "setprop debug.gr.swapinterval 1",
            description = "Disables Display GR Swapinterval for fastest rendering latency",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Game Interpolation 120 FPS",
            command = "settings put system gamecube_frame_interpolation 1:3::48:120",
            category = "Gaming",
            icon = Icons.Default.Speed, // FIX: Use ImageVector directly
            canLoopAsSpecialCase = true,
            resetCommand = "settings put system gamecube_frame_interpolation 0:-1:0:0:0",
            description = "Enables frame interpolation to boost game frame rates to 120 FPS, for ultra-smooth gaming.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Game Interpolation 144 FPS",
            command = "settings put system gamecube_frame_interpolation 1:3::48:144",
            category = "Gaming",
            icon = Icons.Default.SportsEsports, // FIX: Use ImageVector directly
            canLoopAsSpecialCase = true,
            resetCommand = "settings put system gamecube_frame_interpolation 0:-1:0:0:0",
            description = "Enables frame interpolation to achieve game frame rates up to 144 FPS, providing the smoothest possible visuals.",
            isCustom = false
        ),
        // Display Features
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Peak Refresh Rate",
            command = "settings put system peak_refresh_rate 144",
            category = "Display",
            icon = Icons.Default.Refresh, // FIX: Use ImageVector directly
            resetCommand = "settings put system peak_refresh_rate -1",
            description = "Forces the display to operate at its peak refresh rate (e.g., 144Hz) for a smoother visual experience.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Screen Refresh Mode\nVIVO 144hz",
            command = "settings put global vivo_screen_refresh_rate_mode 144",
            category = "Display",
            icon = Icons.Default.ScreenRotation, // FIX: Use ImageVector directly
            resetCommand = "settings put global vivo_screen_refresh_rate_mode 120",
            description = "Sets the global screen refresh rate mode to 144Hz for VIVO devices, ensuring maximum display fluidity.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Screen Refresh Mode\nVIVO 120HZ",
            command = "settings put global vivo_screen_refresh_rate_mode 120",
            category = "Display",
            icon = Icons.Default.ScreenRotation, // FIX: Use ImageVector directly
            resetCommand = "settings put global vivo_screen_refresh_rate_mode 120",
            description = "Sets the global screen refresh rate mode to 120Hz for VIVO devices, optimizing for smooth visuals.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Screen Refresh Mode\nVIVO 90HZ",
            command = "settings put global vivo_screen_refresh_rate_mode 90",
            category = "Display",
            icon = Icons.Default.ScreenRotation, // FIX: Use ImageVector directly
            resetCommand = "settings put global vivo_screen_refresh_rate_mode 120",
            description = "Sets the global screen refresh rate mode to 90Hz for VIVO devices, offering a balance between smoothness and battery.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "MEMC Main Switch",
            command = "settings put system memc_main_switch_setting 1",
            category = "Display",
            icon = Icons.Default.MovieFilter, // FIX: Use ImageVector directly
            resetCommand = "settings put system memc_main_switch_setting 0",
            description = "Turns on Motion Estimation, Motion Compensation (MEMC) for smoother video playback by inserting frames.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Nova MEMC Status",
            command = "settings put system nova_memc_status 1",
            category = "Display",
            icon = Icons.Default.MovieFilter, // FIX: Use ImageVector directly
            resetCommand = "settings put system nova_memc_status 0",
            description = "Sets the status of Nova MEMC, a specific implementation of motion compensation.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Nova SR Status",
            command = "settings put system nova_sr_status 1",
            category = "Display",
            icon = Icons.Default.HighQuality, // FIX: Use ImageVector directly
            canLoopAsSpecialCase = true,
            resetCommand = "settings put system nova_sr_status 0",
            description = "Sets the status of Nova Super Resolution (SR), enhancing image clarity and detail.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Vivo SR Status",
            command = "settings put system vivo_sr_status 1",
            category = "Display",
            icon = Icons.Default.HighQuality, // FIX: Use ImageVector directly
            canLoopAsSpecialCase = true,
            resetCommand = "settings put system vivo_sr_status 0",
            description = "Sets the status of Vivo's proprietary Super Resolution (SR) technology.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "SDR2HDR Software Switch",
            command = "settings put system sdr2hdr_software_user_switch_setting 0",
            category = "Display",
            icon = Icons.Default.BrightnessHigh, // FIX: Use ImageVector directly
            resetCommand = null,
            description = "Controls the software switch for SDR to HDR conversion, enhancing dynamic range for standard content.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "SDR2HDR High Temp",
            command = "settings put system sdr2hdr_is_high_temp_setting 0",
            category = "Display",
            icon = Icons.Default.BrightnessHigh, // FIX: Use ImageVector directly
            resetCommand = null,
            description = "Sets the high temperature flag for SDR2HDR, potentially affecting its operation under heat.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "SDR2HDR Main Switch",
            command = "settings put system sdr2hdr_main_switch_setting 0",
            category = "Display",
            icon = Icons.Default.BrightnessHigh, // FIX: Use ImageVector directly
            resetCommand = null,
            description = "The main switch for enabling or disabling the SDR to HDR conversion feature.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Disable LTPO Lock Refresh Rate",
            command = "settings put system vivo_ltpo_setting 1",
            category = "Display",
            icon = Icons.Default.LockOpen, // FIX: Use ImageVector directly
            resetCommand = "settings put system vivo_ltpo_setting 0",
            description = "Disables the LTPO (Low-Temperature Polycrystalline Oxide) display's ability to dynamically lock refresh rates, potentially forcing a higher fixed rate.",
            isCustom = false
        ),
        // Touch Features
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Touch Sync",
            command = "settings put secure vivo_sync_enabled_def 1",
            category = "Touch",
            icon = Icons.Default.Sync, // FIX: Use ImageVector directly
            resetCommand = "settings put secure vivo_sync_enabled_def 0",
            description = "Enables Vivo's touch synchronization feature, potentially reducing input lag. This feature improves touch response and accuracy to max.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "☛ Touch Rate 333 ☚\n(Enable with GameMode)",
            command = "settings put global game_memc_request_touch_rate 1",
            category = "Touch",
            icon = Icons.Default.TouchApp, // FIX: Use ImageVector directly
            canLoopAsSpecialCase = true,
            resetCommand = "settings put global game_memc_request_touch_rate 0",
            description = "Activates an ultra-high touch sampling rate (333Hz) for incredibly responsive touch input, especially in games. Requires Game Mode feature to be activated along with this.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Touch Rate 180\n(Enable with GameMode)",
            command = "settings put global game_memc_request_touch_rate 180",
            category = "Touch",
            icon = Icons.Default.TouchApp, // FIX: Use ImageVector directly
            canLoopAsSpecialCase = true,
            resetCommand = "settings put global game_memc_request_touch_rate 0",
            description = "Sets the touch sampling rate to 180Hz for improved touch responsiveness in games. Requires Game Mode feature to be activated along with this.",
            isCustom = false
        ),
        Feature(
            id = UUID.randomUUID().toString(),
            title = "Touch sensitivity and Accuracy",
            command = "settings put system vts_game_para_adjust 0,5,5,5",
            category = "Touch",
            icon = Icons.Default.Gesture, // FIX: Use ImageVector directly
            canLoopAsSpecialCase = true,
            resetCommand = "settings put system vts_game_para_adjust 0,3,3,3",
            description = "Adjusts the device's touch sensitivity and accuracy parameters, often used for gaming optimization.",
            isCustom = false
        )
    )

    // Function to get all features (predefined + custom) as a Flow
    fun getAllFeaturesFlow(context: Context): Flow<List<Feature>> {
        val customFeaturesFlow = DataStoreManager.getCustomFeaturesFlow(context)
        return customFeaturesFlow.map { customFeatures ->
            val combinedMap = predefinedFeatures.associateBy { it.id }.toMutableMap()
            customFeatures.forEach { customFeature ->
                combinedMap[customFeature.id] = customFeature
            }
            combinedMap.values.toList().distinctBy { it.id }
        }
    }

    // This suspend function is generally for internal use or where a direct List is needed (e.g., one-off reads).
    // It is NOT meant to be called directly from @Composable functions in a non-suspending context.
    suspend fun getAllFeatureToggles(): List<Feature> {
        Log.w("FeatureRepository", "getAllFeatureToggles() called. Consider using getAllFeaturesFlow(context) for dynamic features in Composables.")
        return predefinedFeatures
    }

    /**
     * Categorizes a given list of features by their category.
     * @param features The list of features to categorize (can be predefined + custom).
     * @return A Map where keys are categories and values are lists of features in that category.
     */
    fun getCategorizedFeatures(features: List<Feature>): Map<String, List<Feature>> {
        val categorized = features.groupBy { it.category }
        val sortedCategories = categorized.keys.sortedWith(compareBy {
            when (it) {
                "CPU" -> 0
                "GPU" -> 1
                "System" -> 2
                "Gaming" -> 3
                "Display" -> 4
                "Touch" -> 5
                "Thermal" -> 6
                "Custom" -> 7
                "Test" -> 8
                else -> 10
            }
        })
        return sortedCategories.associateWith { category ->
            categorized[category]?.sortedBy { it.title } ?: emptyList()
        }
    }

    // Function to add a custom feature (persists via DataStore)
    suspend fun addCustomFeature(context: Context, feature: Feature) {
        DataStoreManager.saveCustomFeature(context, feature.copy(isCustom = true))
    }

    // Function to delete a custom feature
    suspend fun deleteCustomFeature(context: Context, featureId: String) {
        DataStoreManager.deleteCustomFeature(context, featureId)
    }

    /**
     * Filters features into run-once and loop categories.
     */
    fun filterFeaturesForExecution(features: List<Feature>): Pair<List<Feature>, List<Feature>> {
        val runOnceFeatures = features.filter { !it.requiresLoop && !it.canLoopAsSpecialCase }
        val loopFeatures = features.filter { it.requiresLoop || it.canLoopAsSpecialCase }
        return Pair(runOnceFeatures, loopFeatures)
    }
}