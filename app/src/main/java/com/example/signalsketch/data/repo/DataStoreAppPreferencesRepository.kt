package com.example.signalsketch.data.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreAppPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) : AppPreferencesRepository {
    override val preferences: Flow<AppPreferences> = dataStore.data.map { preferences ->
        AppPreferences(
            selectedColorScale = preferences[SELECTED_COLOR_SCALE]
                ?.toEnumOrNull<ColorScalePreference>()
                ?: ColorScalePreference.VIRIDIS,
            scanIntervalMillis = preferences[SCAN_INTERVAL_MILLIS] ?: 3_000,
            hasCompletedOnboarding = preferences[HAS_COMPLETED_ONBOARDING] ?: false,
            preferredMappingMode = preferences[PREFERRED_MAPPING_MODE]
                ?.toEnumOrNull<PreferredMappingMode>()
                ?: PreferredMappingMode.STANDARD
        )
    }

    override suspend fun setSelectedColorScale(colorScale: ColorScalePreference) {
        dataStore.edit { preferences ->
            preferences[SELECTED_COLOR_SCALE] = colorScale.name
        }
    }

    override suspend fun setScanIntervalMillis(scanIntervalMillis: Int) {
        dataStore.edit { preferences ->
            preferences[SCAN_INTERVAL_MILLIS] = scanIntervalMillis
        }
    }

    override suspend fun setHasCompletedOnboarding(hasCompletedOnboarding: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING] = hasCompletedOnboarding
        }
    }

    override suspend fun setPreferredMappingMode(mappingMode: PreferredMappingMode) {
        dataStore.edit { preferences ->
            preferences[PREFERRED_MAPPING_MODE] = mappingMode.name
        }
    }

    private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? {
        return enumValues<T>().firstOrNull { it.name == this }
    }

    private companion object {
        val SELECTED_COLOR_SCALE = stringPreferencesKey("selected_color_scale")
        val SCAN_INTERVAL_MILLIS = intPreferencesKey("scan_interval_millis")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val PREFERRED_MAPPING_MODE = stringPreferencesKey("preferred_mapping_mode")
    }
}
