package com.example.signalsketch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.signalsketch.sensors.MotionEstimate
import com.example.signalsketch.sensors.MotionTrackingRepository
import com.example.signalsketch.sensors.MotionTrackingRepositoryFactory
import com.example.signalsketch.sensors.MotionTrackingState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MappingSessionUiState(
    val headingDegrees: Float = 0f,
    val deltaXMeters: Float = 0f,
    val deltaYMeters: Float = 0f,
    val sensorSampleCount: Int = 0,
    val trackingState: MotionTrackingState = MotionTrackingState.IDLE
)

class MappingSessionViewModel(
    application: Application,
    private val motionTrackingRepository: MotionTrackingRepository =
        MotionTrackingRepositoryFactory.create(application)
) : AndroidViewModel(application) {
    val uiState: StateFlow<MappingSessionUiState> = motionTrackingRepository.motionEstimate
        .map(::toUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MappingSessionUiState()
        )

    fun startTracking() {
        motionTrackingRepository.startTracking()
    }

    fun stopTracking() {
        motionTrackingRepository.stopTracking()
    }

    override fun onCleared() {
        motionTrackingRepository.close()
        super.onCleared()
    }

    private fun toUiState(estimate: MotionEstimate): MappingSessionUiState {
        return MappingSessionUiState(
            headingDegrees = estimate.headingDegrees,
            deltaXMeters = estimate.movementDelta.deltaXMeters,
            deltaYMeters = estimate.movementDelta.deltaYMeters,
            sensorSampleCount = estimate.sensorSampleCount,
            trackingState = estimate.trackingState
        )
    }
}
