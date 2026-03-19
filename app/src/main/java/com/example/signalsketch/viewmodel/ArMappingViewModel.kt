package com.example.signalsketch.viewmodel

import android.app.Activity
import android.app.Application
import android.view.MotionEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.signalsketch.ar.ArAvailabilityRepositoryFactory
import com.example.signalsketch.ar.ArAvailabilityState
import com.example.signalsketch.ar.ArSessionLifecycleState
import com.example.signalsketch.ar.ArSessionState
import com.example.signalsketch.ar.DefaultArSessionController
import com.example.signalsketch.position.LivePositionSample
import com.example.signalsketch.position.PositionSourceRepositoryFactory
import com.example.signalsketch.position.PositionSourceType
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ArMappingUiState(
    val availability: ArAvailabilityState = ArAvailabilityState(),
    val sessionLifecycleState: ArSessionLifecycleState = ArSessionLifecycleState.IDLE,
    val hasDetectedHorizontalPlane: Boolean = false,
    val anchorCount: Int = 0,
    val lastErrorMessage: String? = null
)

class ArMappingViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val arAvailabilityRepository = ArAvailabilityRepositoryFactory.create(application)
    private val arSessionController = DefaultArSessionController()
    private val positionSourceRepository = PositionSourceRepositoryFactory.create(application)

    val uiState: StateFlow<ArMappingUiState> = combine(
        arAvailabilityRepository.availabilityState,
        arSessionController.sessionState
    ) { availability, session ->
        availability.toUiState(session)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = arAvailabilityRepository.availabilityState.value.toUiState(
            arSessionController.sessionState.value
        )
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

    fun onArSessionCreated() {
        arSessionController.onSessionCreated()
    }

    fun onArSessionResumed() {
        arSessionController.onSessionResumed()
    }

    fun onArSessionPaused() {
        arSessionController.onSessionPaused()
        positionSourceRepository.clearArPosition("AR session paused.")
    }

    fun onArSessionFailed(message: String?) {
        arSessionController.onSessionFailed(message)
        positionSourceRepository.clearArPosition(message ?: "AR session failed.")
    }

    fun onArFrameUpdated(frame: Frame) {
        arSessionController.onFrameUpdated(frame)
        val sessionState = arSessionController.sessionState.value
        val x = sessionState.xMeters ?: return
        val y = sessionState.yMeters ?: return
        val headingDegrees = sessionState.headingDegrees ?: return
        positionSourceRepository.updateArPosition(
            LivePositionSample(
                sourceType = PositionSourceType.AR,
                xMeters = x,
                yMeters = y,
                headingDegrees = headingDegrees,
                trackingQuality = sessionState.trackingQuality,
                status = sessionState.trackingStatus,
                sequence = frame.timestamp,
                recordedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    fun onArTap(frame: Frame, motionEvent: MotionEvent): Anchor? {
        return arSessionController.placeAnchor(frame, motionEvent)
    }

    fun resetSessionState() {
        arSessionController.reset()
    }

    private fun ArAvailabilityState.toUiState(session: ArSessionState): ArMappingUiState {
        return ArMappingUiState(
            availability = this,
            sessionLifecycleState = session.lifecycleState,
            hasDetectedHorizontalPlane = session.hasDetectedHorizontalPlane,
            anchorCount = session.anchorCount,
            lastErrorMessage = session.lastErrorMessage
        )
    }
}
