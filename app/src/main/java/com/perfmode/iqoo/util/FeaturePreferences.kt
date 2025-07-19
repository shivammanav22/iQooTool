package com.perfmode.iqoo.util

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object FeaturePreferences {

    private const val DATASTORE_NAME = "feature_toggle_prefs"

    // Extension to get DataStore from any Context
    private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

    /**
     * Save toggle state for a given feature key.
     */
    suspend fun saveFeatureToggle(context: Context, key: String, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(key)] = enabled
        }
    }

    /**
     * Observe toggle state changes for a given feature key.
     */
    fun getFeatureToggleFlow(context: Context, key: String): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[booleanPreferencesKey(key)] ?: false
        }
    }
}
