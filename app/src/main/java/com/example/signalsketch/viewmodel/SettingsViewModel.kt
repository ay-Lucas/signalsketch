package com.example.signalsketch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.signalsketch.data.repo.AppPreferences
import com.example.signalsketch.data.repo.ColorScalePreference
import com.example.signalsketch.data.repo.DataStoreAppPreferencesRepository
import com.example.signalsketch.data.repo.PreferredMappingMode
import com.example.signalsketch.storage.appPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val colorScale: String = "",
    val preferredMappingMode: String = "",
    val scanIntervalLabel: String = "",
    val onboardingLabel: String = ""
)

class SettingsViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository = DataStoreAppPreferencesRepository(application.appPreferencesDataStore)

    val uiState: StateFlow<SettingsUiState> = repository.preferences
        .map { preferences -> preferences.toUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState()
        )

    fun cycleColorScale() {
        viewModelScope.launch {
            val current = repository.preferences.first().selectedColorScale
            val next = when (current) {
                ColorScalePreference.VIRIDIS -> ColorScalePreference.TURBO
                ColorScalePreference.TURBO -> ColorScalePreference.GRAYSCALE
                ColorScalePreference.GRAYSCALE -> ColorScalePreference.VIRIDIS
            }
            repository.setSelectedColorScale(next)
        }
    }

    fun cyclePreferredMappingMode() {
        viewModelScope.launch {
            val current = repository.preferences.first().preferredMappingMode
            val next = when (current) {
                PreferredMappingMode.STANDARD -> PreferredMappingMode.AR
                PreferredMappingMode.AR -> PreferredMappingMode.STANDARD
            }
            repository.setPreferredMappingMode(next)
        }
    }

    fun cycleScanInterval() {
        viewModelScope.launch {
            val current = repository.preferences.first().scanIntervalMillis
            val next = when (current) {
                2_000 -> 3_000
                3_000 -> 5_000
                else -> 2_000
            }
            repository.setScanIntervalMillis(next)
        }
    }

    fun toggleOnboardingComplete() {
        viewModelScope.launch {
            val current = repository.preferences.first().hasCompletedOnboarding
            repository.setHasCompletedOnboarding(!current)
        }
    }
}

private fun AppPreferences.toUiState(): SettingsUiState {
    return SettingsUiState(
        isLoading = false,
        colorScale = selectedColorScale.name.lowercase().replaceFirstChar(Char::titlecase),
        preferredMappingMode = preferredMappingMode.name.lowercase().replaceFirstChar(Char::titlecase),
        scanIntervalLabel = "${scanIntervalMillis / 1000} seconds",
        onboardingLabel = if (hasCompletedOnboarding) "Completed" else "Not completed"
    )
}
