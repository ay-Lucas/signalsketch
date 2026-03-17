package com.example.signalsketch.data.repo

import kotlinx.coroutines.flow.Flow

enum class ColorScalePreference {
    VIRIDIS,
    TURBO,
    GRAYSCALE
}

enum class PreferredMappingMode {
    STANDARD,
    AR
}

data class AppPreferences(
    val selectedColorScale: ColorScalePreference = ColorScalePreference.VIRIDIS,
    val scanIntervalMillis: Int = 3_000,
    val hasCompletedOnboarding: Boolean = false,
    val preferredMappingMode: PreferredMappingMode = PreferredMappingMode.STANDARD
)

interface AppPreferencesRepository {
    val preferences: Flow<AppPreferences>

    suspend fun setSelectedColorScale(colorScale: ColorScalePreference)

    suspend fun setScanIntervalMillis(scanIntervalMillis: Int)

    suspend fun setHasCompletedOnboarding(hasCompletedOnboarding: Boolean)

    suspend fun setPreferredMappingMode(mappingMode: PreferredMappingMode)
}
