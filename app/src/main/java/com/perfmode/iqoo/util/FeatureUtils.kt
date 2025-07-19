package com.perfmode.iqoo.util

import android.content.Context // NEW: Import Context
import com.perfmode.iqoo.model.Feature // NEW: Import Feature
import kotlinx.coroutines.flow.first // NEW: Import first

/**
 * Returns a list of commands for all features that are currently enabled based on the provided toggles.
 * This function correctly filters both predefined and custom features.
 *
 * @param context The application context, needed to access DataStore via FeatureRepository.
 * @param toggles A map of feature titles to their enabled state.
 * @return A list of commands for the enabled features.
 */
suspend fun getAllEnabledToggles(context: Context, toggles: Map<String, Boolean>): List<String> { // FIX: Made suspend and added Context
    // FIX: Use getAllFeaturesFlow(context).first() to get the combined list of all features (predefined + custom)
    val allFeatures = FeatureRepository.getAllFeaturesFlow(context).first()
    return allFeatures
        .filter { toggles[it.title] == true } // Filter by enabled toggles
        .map { it.command } // Map to their commands
}

/**
 * Returns a list of commands for features enabled based on the provided toggles.
 * This function simply delegates to getAllEnabledToggles.
 *
 * @param context The application context, needed to access DataStore via FeatureRepository.
 * @param toggles A map of feature titles to their enabled state.
 * @return A list of commands for the enabled features.
 */
suspend fun getEnabledCommandsFromToggles(context: Context, toggles: Map<String, Boolean>): List<String> { // FIX: Made suspend and added Context
    return getAllEnabledToggles(context, toggles) // Pass context
}