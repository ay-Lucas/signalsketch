package com.example.signalsketch.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.signalsketch.ar.ArAvailabilityRepository
import com.example.signalsketch.ar.ArAvailabilityRepositoryFactory
import com.example.signalsketch.ar.ArAvailabilityState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ArMappingViewModel(
    application: Application,
    private val arAvailabilityRepository: ArAvailabilityRepository =
        ArAvailabilityRepositoryFactory.create(application)
) : AndroidViewModel(application) {
    val uiState: StateFlow<ArAvailabilityState> = arAvailabilityRepository.availabilityState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = arAvailabilityRepository.availabilityState.value
        )

    fun refresh() {
        arAvailabilityRepository.refreshAvailability()
        arAvailabilityRepository.refreshCameraPermission()
    }

    fun onCameraPermissionResult() {
        arAvailabilityRepository.refreshCameraPermission()
        arAvailabilityRepository.refreshAvailability()
    }

    fun requestArInstall(activity: Activity) {
        arAvailabilityRepository.requestInstall(activity)
    }
}
