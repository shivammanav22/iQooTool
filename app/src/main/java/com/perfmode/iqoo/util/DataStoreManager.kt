package com.perfmode.iqoo.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import com.google.gson.Gson
import com.perfmode.iqoo.model.Feature
import android.util.Log
import com.google.gson.GsonBuilder
import androidx.compose.ui.graphics.vector.ImageVector // Make sure this import is here

// Extension property to create a DataStore instance for preferences.
// This must be outside the object DataStoreManager block.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "feature_prefs")

/**
 * Manages data persistence using Jetpack DataStore for various application settings,
 * including feature toggles, active looping commands, and selected overlay features.
 */
object DataStoreManager {

    // --- Preference Keys ---
    private val ACTIVE_LOOP_COMMANDS_KEY = stringSetPreferencesKey("active_loop_commands")
    private val OVERLAY_FEATURES_KEY = stringSetPreferencesKey("overlay_feature_titles")
    private val LOOP_DELAY_KEY = longPreferencesKey("loop_delay_millis")
    private val CUSTOM_FEATURES_KEY = stringSetPreferencesKey("custom_features_json")

    // Gson instance with custom TypeAdapter for ImageVector
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(ImageVector::class.java, ImageVectorAdapter())
        .create()

    // --- Feature Toggles Management ---
    fun getFeatureTogglesFlow(context: Context): Flow<Map<String, Boolean>> {
        return context.dataStore.data.map { prefs ->
            prefs.asMap().mapNotNull { entry ->
                if (entry.value is Boolean) {
                    entry.key.name to (entry.value as Boolean)
                } else {
                    null
                }
            }.toMap()
        }
    }

    suspend fun setFeatureEnabled(context: Context, featureTitle: String, isEnabled: Boolean) {
        context.dataStore.edit { prefs ->
            val key = booleanPreferencesKey(featureTitle)
            prefs[key] = isEnabled
        }
    }

    // --- Active Looping Commands Management ---
    suspend fun saveActiveLoopCommands(context: Context, commands: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_LOOP_COMMANDS_KEY] = commands.toSet()
        }
    }

    suspend fun getActiveLoopCommands(context: Context): List<String> {
        return context.dataStore.data.first()[ACTIVE_LOOP_COMMANDS_KEY]?.toList() ?: emptyList()
    }

    suspend fun clearActiveLoopCommands(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(ACTIVE_LOOP_COMMANDS_KEY)
        }
    }

    // --- Overlay Feature Selection Management ---
    suspend fun saveSelectedOverlayFeatures(context: Context, featureTitles: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[OVERLAY_FEATURES_KEY] = featureTitles
        }
    }

    fun getSelectedOverlayFeaturesFlow(context: Context): Flow<Set<String>> {
        return context.dataStore.data.map { prefs ->
            prefs[OVERLAY_FEATURES_KEY] ?: emptySet()
        }
    }

    // --- Loop Delay Management ---
    suspend fun saveLoopDelay(context: Context, delayMillis: Long) {
        context.dataStore.edit { preferences ->
            preferences[LOOP_DELAY_KEY] = delayMillis
        }
    }

    fun getLoopDelayFlow(context: Context): Flow<Long> {
        return context.dataStore.data
            .map { preferences ->
                preferences[LOOP_DELAY_KEY] ?: 5000L
            }
    }

    // --- Custom Feature Management ---
    /**
     * Saves a new custom feature to DataStore.
     * @param context The application context.
     * @param feature The Feature object to save. It should have isCustom = true.
     */
    suspend fun saveCustomFeature(context: Context, feature: Feature) {
        context.dataStore.edit { prefs ->
            val currentCustomFeaturesJson = prefs[CUSTOM_FEATURES_KEY]?.toMutableSet() ?: mutableSetOf()
            currentCustomFeaturesJson.removeAll { json ->
                try {
                    val existingFeature = gson.fromJson(json, Feature::class.java)
                    existingFeature.id == feature.id
                } catch (e: Exception) {
                    Log.e("DataStoreManager", "Error parsing existing custom feature JSON during update/save: $json", e)
                    false
                }
            }
            val featureJson = gson.toJson(feature)
            currentCustomFeaturesJson.add(featureJson)
            prefs[CUSTOM_FEATURES_KEY] = currentCustomFeaturesJson
            Log.d("DataStoreManager", "Saved custom feature: ${feature.title}, JSON: $featureJson")
        }
    }

    /**
     * Deletes a custom feature from DataStore by its ID.
     * @param context The application context.
     * @param featureId The ID of the feature to delete.
     */
    suspend fun deleteCustomFeature(context: Context, featureId: String) {
        context.dataStore.edit { prefs ->
            val currentCustomFeaturesJson = prefs[CUSTOM_FEATURES_KEY]?.toMutableSet() ?: mutableSetOf()
            currentCustomFeaturesJson.removeAll { json ->
                try {
                    val feature = gson.fromJson(json, Feature::class.java)
                    feature.id == featureId
                } catch (e: Exception) {
                    Log.e("DataStoreManager", "Error parsing custom feature JSON during deletion: $json", e)
                    false
                }
            }
            prefs[CUSTOM_FEATURES_KEY] = currentCustomFeaturesJson
        }
    }

    /**
     * Retrieves all custom features from DataStore as a Flow of List.
     * @param context The application context.
     * @return A Flow emitting a List of custom Feature objects.
     */
    fun getCustomFeaturesFlow(context: Context): Flow<List<Feature>> {
        return context.dataStore.data
            .map { preferences ->
                val customFeaturesJson = preferences[CUSTOM_FEATURES_KEY] ?: emptySet()
                customFeaturesJson.mapNotNull { json ->
                    try {
                        val parsedFeature = gson.fromJson(json, Feature::class.java)
                        Log.d("DataStoreManager", "Parsed custom feature: ${parsedFeature.title}, JSON: $json")
                        parsedFeature
                    } catch (e: Exception) {
                        Log.e("DataStoreManager", "CRASH LIKELY HERE: Error parsing custom feature JSON: $json", e)
                        null
                    }
                }
            }
    }
}