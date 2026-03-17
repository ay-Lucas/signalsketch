package com.example.signalsketch.ar

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

interface ArAvailabilityRepository {
    val availabilityState: StateFlow<ArAvailabilityState>

    fun refreshAvailability()

    fun refreshCameraPermission()

    fun requestInstall(activity: Activity)

    fun markInstallFlowRetried()
}
