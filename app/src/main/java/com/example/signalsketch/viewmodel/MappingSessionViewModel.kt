package com.example.signalsketch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.signalsketch.sensors.MotionEstimate
import com.example.signalsketch.sensors.MotionTrackingRepository
import com.example.signalsketch.sensors.MotionTrackingRepositoryFactory
import com.example.signalsketch.sensors.MotionTrackingState
import com.example.signalsketch.wifi.WifiPermissionStatus
import com.example.signalsketch.wifi.WifiRepository
import com.example.signalsketch.wifi.WifiRepositoryFactory
import com.example.signalsketch.wifi.WifiScanSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MappingSessionViewModel(
    application: Application,
    private val motionTrackingRepository: MotionTrackingRepository =
        MotionTrackingRepositoryFactory.create(application),
    private val wifiRepository: WifiRepository = WifiRepositoryFactory.create(application)
) : AndroidViewModel(application) {
    private val sessionState = MutableStateFlow(SessionRecordingState())

    val uiState: StateFlow<MappingSessionUiState> = combine(
        sessionState,
        motionTrackingRepository.motionEstimate,
        wifiRepository.scanSnapshot,
        wifiRepository.permissionStatus
    ) { session, motionEstimate, wifiSnapshot, wifiPermission ->
        buildUiState(session, motionEstimate, wifiSnapshot, wifiPermission)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MappingSessionUiState()
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
        sessionState.value = SessionRecordingState(
            lifecycleState = RecordingSessionState.ACTIVE,
            sessionId = now,
            sessionStartedAtEpochMillis = now,
            statusMessage = "Session started."
        )
    }

    fun pauseSession() {
        val now = System.currentTimeMillis()
        motionTrackingRepository.stopTracking()
        wifiRepository.stopScan()
        sessionState.update { current ->
            current.copy(
                lifecycleState = RecordingSessionState.PAUSED,
                sessionPausedAtEpochMillis = now,
                statusMessage = "Session paused."
            )
        }
    }

    fun resumeSession() {
        motionTrackingRepository.startTracking()
        wifiRepository.startScan()
        sessionState.update { current ->
            current.copy(
                lifecycleState = RecordingSessionState.ACTIVE,
                sessionPausedAtEpochMillis = null,
                statusMessage = "Session resumed."
            )
        }
    }

    fun resetSession() {
        motionTrackingRepository.stopTracking()
        wifiRepository.stopScan()
        sessionState.value = SessionRecordingState(statusMessage = "Session reset.")
    }

    override fun onCleared() {
        motionTrackingRepository.close()
        wifiRepository.close()
        super.onCleared()
    }

    private fun observeMotionSamples() {
        viewModelScope.launch {
            motionTrackingRepository.motionEstimate.collect { motionEstimate ->
                sessionState.update { current ->
                    if (current.lifecycleState != RecordingSessionState.ACTIVE) {
                        return@update current
                    }
                    if (motionEstimate.sensorSampleCount <= current.lastRecordedMotionSampleCount) {
                        return@update current
                    }

                    current.copy(
                        pathSamples = current.pathSamples + RecordedPathSample(
                            index = current.pathSamples.size,
                            xMeters = motionEstimate.movementDelta.deltaXMeters,
                            yMeters = motionEstimate.movementDelta.deltaYMeters,
                            headingDegrees = motionEstimate.headingDegrees,
                            sensorSampleCount = motionEstimate.sensorSampleCount,
                            recordedAtEpochMillis = System.currentTimeMillis()
                        ),
                        lastRecordedMotionSampleCount = motionEstimate.sensorSampleCount,
                        lastPathCaptureAtEpochMillis = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    private fun observeWifiSamples() {
        viewModelScope.launch {
            wifiRepository.scanSnapshot.collect { wifiSnapshot ->
                sessionState.update { current ->
                    if (current.lifecycleState != RecordingSessionState.ACTIVE) {
                        return@update current
                    }
                    val completedAt = wifiSnapshot.lastScanCompletedAtEpochMillis
                        ?: return@update current
                    if (completedAt <= current.lastRecordedWifiSnapshotAtEpochMillis) {
                        return@update current
                    }

                    val now = System.currentTimeMillis()
                    val recordedSamples = wifiSnapshot.visibleNetworks.map { network ->
                        RecordedWifiSample(
                            bssid = network.bssid,
                            ssid = network.ssid,
                            rssiDbm = network.rssiDbm,
                            frequencyMhz = network.frequencyMhz,
                            timestampMicros = network.timestampMicros,
                            xMeters = current.latestX,
                            yMeters = current.latestY,
                            headingDegrees = current.latestHeadingDegrees,
                            recordedAtEpochMillis = now
                        )
                    }

                    current.copy(
                        wifiSamples = current.wifiSamples + recordedSamples,
                        lastRecordedWifiSnapshotAtEpochMillis = completedAt,
                        lastWifiCaptureAtEpochMillis = completedAt
                    )
                }
            }
        }
    }

    private fun buildUiState(
        session: SessionRecordingState,
        motionEstimate: MotionEstimate,
        wifiSnapshot: WifiScanSnapshot,
        wifiPermission: WifiPermissionStatus
    ): MappingSessionUiState {
        val statusMessage = when {
            session.statusMessage != null -> session.statusMessage
            session.lifecycleState == RecordingSessionState.ACTIVE && !wifiPermission.isGranted -> {
                "Recording is active, but Wi-Fi permissions are incomplete."
            }
            session.lifecycleState == RecordingSessionState.ACTIVE &&
                motionEstimate.trackingState == MotionTrackingState.SENSOR_UNAVAILABLE -> {
                "Required motion sensors are unavailable on this device."
            }
            session.lifecycleState == RecordingSessionState.ACTIVE && wifiSnapshot.isScanning -> {
                "Recording live Wi-Fi and motion samples."
            }
            else -> null
        }

        return MappingSessionUiState(
            sessionState = session.lifecycleState,
            sessionId = session.sessionId,
            headingDegrees = motionEstimate.headingDegrees,
            deltaXMeters = motionEstimate.movementDelta.deltaXMeters,
            deltaYMeters = motionEstimate.movementDelta.deltaYMeters,
            sensorSampleCount = motionEstimate.sensorSampleCount,
            trackingState = motionEstimate.trackingState,
            wifiSampleCount = session.wifiSamples.size,
            pathSampleCount = session.pathSamples.size,
            sessionStartedAtEpochMillis = session.sessionStartedAtEpochMillis,
            sessionPausedAtEpochMillis = session.sessionPausedAtEpochMillis,
            lastWifiCaptureAtEpochMillis = session.lastWifiCaptureAtEpochMillis,
            lastPathCaptureAtEpochMillis = session.lastPathCaptureAtEpochMillis,
            statusMessage = statusMessage
        )
    }
}

private data class SessionRecordingState(
    val lifecycleState: RecordingSessionState = RecordingSessionState.IDLE,
    val sessionId: Long? = null,
    val sessionStartedAtEpochMillis: Long? = null,
    val sessionPausedAtEpochMillis: Long? = null,
    val pathSamples: List<RecordedPathSample> = emptyList(),
    val wifiSamples: List<RecordedWifiSample> = emptyList(),
    val lastRecordedMotionSampleCount: Int = 0,
    val lastRecordedWifiSnapshotAtEpochMillis: Long = 0,
    val lastWifiCaptureAtEpochMillis: Long? = null,
    val lastPathCaptureAtEpochMillis: Long? = null,
    val statusMessage: String? = null
) {
    val latestX: Float
        get() = pathSamples.lastOrNull()?.xMeters ?: 0f

    val latestY: Float
        get() = pathSamples.lastOrNull()?.yMeters ?: 0f

    val latestHeadingDegrees: Float
        get() = pathSamples.lastOrNull()?.headingDegrees ?: 0f
}
