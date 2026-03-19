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
import com.example.signalsketch.position.PositionSourceState
import com.example.signalsketch.position.PositionSourceType
import com.example.signalsketch.position.TrackingQuality
import com.example.signalsketch.sensors.MotionEstimate
import com.example.signalsketch.sensors.MotionTrackingRepositoryFactory
import com.example.signalsketch.sensors.MotionTrackingState
import com.example.signalsketch.wifi.VisibleWifiNetwork
import com.example.signalsketch.wifi.WifiRepositoryFactory
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArSampleMarkerUiState(
    val id: Long,
    val xMeters: Float,
    val yMeters: Float,
    val signalLabel: String,
    val colorArgb: Int
)

data class ArMappingUiState(
    val availability: ArAvailabilityState = ArAvailabilityState(),
    val sessionLifecycleState: ArSessionLifecycleState = ArSessionLifecycleState.IDLE,
    val hasDetectedHorizontalPlane: Boolean = false,
    val anchorCount: Int = 0,
    val lastErrorMessage: String? = null,
    val recordingSessionState: RecordingSessionState = RecordingSessionState.IDLE,
    val wifiSampleCount: Int = 0,
    val liveSampleMarkers: List<ArSampleMarkerUiState> = emptyList(),
    val preferredPositionSource: PositionSourceType = PositionSourceType.NONE,
    val trackingQuality: TrackingQuality = TrackingQuality.UNAVAILABLE,
    val trackingStatus: String = "AR session inactive.",
    val statusMessage: String? = null
) {
    val canStartSession: Boolean
        get() = recordingSessionState == RecordingSessionState.IDLE

    val canPauseSession: Boolean
        get() = recordingSessionState == RecordingSessionState.ACTIVE

    val canResumeSession: Boolean
        get() = recordingSessionState == RecordingSessionState.PAUSED

    val canResetSession: Boolean
        get() = recordingSessionState != RecordingSessionState.IDLE || liveSampleMarkers.isNotEmpty()
}

class ArMappingViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val arAvailabilityRepository = ArAvailabilityRepositoryFactory.create(application)
    private val arSessionController = DefaultArSessionController()
    private val positionSourceRepository = PositionSourceRepositoryFactory.create(application)
    private val motionTrackingRepository = MotionTrackingRepositoryFactory.create(application)
    private val wifiRepository = WifiRepositoryFactory.create(application)
    private val sessionState = MutableStateFlow(ArRecordingSessionState())

    val uiState: StateFlow<ArMappingUiState> = combine(
        arAvailabilityRepository.availabilityState,
        arSessionController.sessionState,
        sessionState,
        positionSourceRepository.positionSourceState
    ) { availability, session, recordingState, positionSourceState ->
        availability.toUiState(session, recordingState, positionSourceState)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = arAvailabilityRepository.availabilityState.value.toUiState(
            arSessionController.sessionState.value,
            sessionState.value,
            positionSourceRepository.positionSourceState.value
        )
    )

    init {
        wifiRepository.refreshPermissions()
        wifiRepository.refreshConnectedNetwork()
        wifiRepository.refreshScanResults()
        observeMotionSamples()
        observeWifiSamples()
    }

    fun startSession() {
        val now = System.currentTimeMillis()
        motionTrackingRepository.startTracking()
        wifiRepository.startScan()
        sessionState.value = ArRecordingSessionState(
            lifecycleState = RecordingSessionState.ACTIVE,
            sessionId = now,
            statusMessage = "AR mapping session started."
        )
    }

    fun pauseSession() {
        motionTrackingRepository.stopTracking()
        wifiRepository.stopScan()
        sessionState.update { current ->
            current.copy(
                lifecycleState = RecordingSessionState.PAUSED,
                statusMessage = "AR mapping session paused."
            )
        }
    }

    fun resumeSession() {
        motionTrackingRepository.startTracking()
        wifiRepository.startScan()
        sessionState.update { current ->
            current.copy(
                lifecycleState = RecordingSessionState.ACTIVE,
                statusMessage = "AR mapping session resumed."
            )
        }
    }

    fun resetSession() {
        motionTrackingRepository.stopTracking()
        wifiRepository.stopScan()
        sessionState.value = ArRecordingSessionState(
            statusMessage = "AR mapping session reset."
        )
    }

    fun refresh() {
        arAvailabilityRepository.refreshAvailability()
        arAvailabilityRepository.refreshCameraPermission()
        wifiRepository.refreshPermissions()
        wifiRepository.refreshConnectedNetwork()
        wifiRepository.refreshScanResults()
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
        val arState = arSessionController.sessionState.value
        val x = arState.xMeters ?: return
        val y = arState.yMeters ?: return
        val headingDegrees = arState.headingDegrees ?: return
        positionSourceRepository.updateArPosition(
            LivePositionSample(
                sourceType = PositionSourceType.AR,
                xMeters = x,
                yMeters = y,
                headingDegrees = headingDegrees,
                trackingQuality = arState.trackingQuality,
                status = arState.trackingStatus,
                sequence = frame.timestamp,
                recordedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    fun onArTap(frame: Frame, motionEvent: MotionEvent): Anchor? {
        return arSessionController.placeAnchor(frame, motionEvent)
    }

    override fun onCleared() {
        motionTrackingRepository.close()
        wifiRepository.close()
        super.onCleared()
    }

    private fun observeMotionSamples() {
        viewModelScope.launch {
            motionTrackingRepository.motionEstimate.collect { motionEstimate ->
                positionSourceRepository.updateSensorPosition(
                    motionEstimate.toSensorPositionSample()
                )
            }
        }
    }

    private fun observeWifiSamples() {
        viewModelScope.launch {
            combine(
                wifiRepository.scanSnapshot,
                positionSourceRepository.positionSourceState
            ) { wifiSnapshot, positionSourceState ->
                wifiSnapshot to positionSourceState
            }.collect { (wifiSnapshot, positionSourceState) ->
                sessionState.update { current ->
                    if (current.lifecycleState != RecordingSessionState.ACTIVE) {
                        return@update current
                    }
                    val completedAt = wifiSnapshot.lastScanCompletedAtEpochMillis
                        ?: return@update current
                    if (completedAt <= current.lastRecordedWifiSnapshotAtEpochMillis) {
                        return@update current
                    }

                    val preferredSample = positionSourceState.preferredSample ?: return@update current
                    val strongestSignal = wifiSnapshot.visibleNetworks.maxByOrNull { it.rssiDbm }
                    val marker = strongestSignal?.toMarker(preferredSample, completedAt)

                    current.copy(
                        wifiSampleCount = current.wifiSampleCount + wifiSnapshot.visibleNetworks.size,
                        sampleMarkers = current.sampleMarkers + listOfNotNull(marker),
                        lastRecordedWifiSnapshotAtEpochMillis = completedAt,
                        statusMessage = if (positionSourceState.preferredSourceType == PositionSourceType.AR) {
                            "Recorded Wi-Fi samples using AR position."
                        } else {
                            "Recorded Wi-Fi samples using sensor fallback."
                        }
                    )
                }
            }
        }
    }

    private fun ArAvailabilityState.toUiState(
        session: ArSessionState,
        recordingState: ArRecordingSessionState,
        positionSourceState: PositionSourceState
    ): ArMappingUiState {
        val resolvedStatusMessage = when {
            !canStartAr -> statusMessage
            recordingState.statusMessage != null -> recordingState.statusMessage
            positionSourceState.trackingQuality == TrackingQuality.LIMITED -> {
                "Tracking is limited. Move slowly and keep the floor in view."
            }
            positionSourceState.trackingQuality == TrackingQuality.UNAVAILABLE -> {
                "Tracking is lost. Re-scan the floor and improve lighting."
            }
            else -> positionSourceState.status
        }

        return ArMappingUiState(
            availability = this,
            sessionLifecycleState = session.lifecycleState,
            hasDetectedHorizontalPlane = session.hasDetectedHorizontalPlane,
            anchorCount = session.anchorCount,
            lastErrorMessage = session.lastErrorMessage,
            recordingSessionState = recordingState.lifecycleState,
            wifiSampleCount = recordingState.wifiSampleCount,
            liveSampleMarkers = recordingState.sampleMarkers,
            preferredPositionSource = positionSourceState.preferredSourceType,
            trackingQuality = positionSourceState.trackingQuality,
            trackingStatus = positionSourceState.status,
            statusMessage = resolvedStatusMessage
        )
    }

    private fun MotionEstimate.toSensorPositionSample(): LivePositionSample {
        val quality = when (trackingState) {
            MotionTrackingState.TRACKING -> TrackingQuality.LIMITED
            MotionTrackingState.IDLE -> TrackingQuality.UNAVAILABLE
            MotionTrackingState.SENSOR_UNAVAILABLE -> TrackingQuality.UNAVAILABLE
        }
        val status = when (trackingState) {
            MotionTrackingState.TRACKING -> "Sensor-based position fallback is active."
            MotionTrackingState.IDLE -> "Sensor-based position fallback is idle."
            MotionTrackingState.SENSOR_UNAVAILABLE -> "Sensor-based fallback is unavailable."
        }

        return LivePositionSample(
            sourceType = PositionSourceType.SENSORS,
            xMeters = movementDelta.deltaXMeters,
            yMeters = movementDelta.deltaYMeters,
            headingDegrees = headingDegrees,
            trackingQuality = quality,
            status = status,
            sequence = sensorSampleCount.toLong(),
            recordedAtEpochMillis = System.currentTimeMillis()
        )
    }

    private fun VisibleWifiNetwork.toMarker(
        sample: LivePositionSample,
        completedAt: Long
    ): ArSampleMarkerUiState {
        return ArSampleMarkerUiState(
            id = completedAt,
            xMeters = sample.xMeters,
            yMeters = sample.yMeters,
            signalLabel = when {
                rssiDbm >= -55 -> "Strong"
                rssiDbm >= -67 -> "Medium"
                else -> "Weak"
            },
            colorArgb = when {
                rssiDbm >= -55 -> 0xFF2E7D32.toInt()
                rssiDbm >= -67 -> 0xFFF9A825.toInt()
                else -> 0xFFC62828.toInt()
            }
        )
    }
}

private data class ArRecordingSessionState(
    val lifecycleState: RecordingSessionState = RecordingSessionState.IDLE,
    val sessionId: Long? = null,
    val wifiSampleCount: Int = 0,
    val sampleMarkers: List<ArSampleMarkerUiState> = emptyList(),
    val lastRecordedWifiSnapshotAtEpochMillis: Long = 0,
    val statusMessage: String? = null
)
