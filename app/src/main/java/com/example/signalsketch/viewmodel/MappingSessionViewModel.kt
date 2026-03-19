package com.example.signalsketch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.signalsketch.position.LivePositionSample
import com.example.signalsketch.position.PositionSourceRepositoryFactory
import com.example.signalsketch.position.PositionSourceState
import com.example.signalsketch.position.PositionSourceType
import com.example.signalsketch.position.TrackingQuality
import com.example.signalsketch.sensors.MotionEstimate
import com.example.signalsketch.sensors.MotionTrackingRepositoryFactory
import com.example.signalsketch.sensors.MotionTrackingState
import com.example.signalsketch.wifi.WifiPermissionStatus
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
    application: Application
) : AndroidViewModel(application) {
    private val motionTrackingRepository = MotionTrackingRepositoryFactory.create(application)
    private val wifiRepository = WifiRepositoryFactory.create(application)
    private val positionSourceRepository = PositionSourceRepositoryFactory.create(application)
    private val sessionState = MutableStateFlow(SessionRecordingState())

    val uiState: StateFlow<MappingSessionUiState> = combine(
        sessionState,
        motionTrackingRepository.motionEstimate,
        wifiRepository.scanSnapshot,
        wifiRepository.permissionStatus,
        positionSourceRepository.positionSourceState
    ) { session, motionEstimate, wifiSnapshot, wifiPermission, positionSourceState ->
        buildUiState(session, motionEstimate, wifiSnapshot, wifiPermission, positionSourceState)
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
        observePositionSamples()
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
                    current.copy(lastSensorSampleCount = motionEstimate.sensorSampleCount)
                }
                positionSourceRepository.updateSensorPosition(
                    motionEstimate.toSensorPositionSample()
                )
            }
        }
    }

    private fun observePositionSamples() {
        viewModelScope.launch {
            positionSourceRepository.positionSourceState.collect { positionSourceState ->
                val sample = positionSourceState.preferredSample ?: return@collect
                sessionState.update { current ->
                    if (current.lifecycleState != RecordingSessionState.ACTIVE) {
                        return@update current
                    }
                    if (sample.sequence <= current.lastRecordedPositionSequence) {
                        return@update current
                    }

                    current.copy(
                        pathSamples = current.pathSamples + RecordedPathSample(
                            index = current.pathSamples.size,
                            xMeters = sample.xMeters,
                            yMeters = sample.yMeters,
                            headingDegrees = sample.headingDegrees,
                            sensorSampleCount = current.lastSensorSampleCount,
                            recordedAtEpochMillis = sample.recordedAtEpochMillis
                        ),
                        lastRecordedPositionSequence = sample.sequence,
                        lastPositionSourceType = sample.sourceType,
                        lastPathCaptureAtEpochMillis = sample.recordedAtEpochMillis
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
        wifiPermission: WifiPermissionStatus,
        positionSourceState: PositionSourceState
    ): MappingSessionUiState {
        val statusMessage = when {
            session.statusMessage != null -> session.statusMessage
            session.lifecycleState == RecordingSessionState.ACTIVE && !wifiPermission.isGranted -> {
                "Recording is active, but Wi-Fi permissions are incomplete."
            }
            session.lifecycleState == RecordingSessionState.ACTIVE &&
                motionEstimate.trackingState == MotionTrackingState.SENSOR_UNAVAILABLE &&
                positionSourceState.preferredSourceType != PositionSourceType.AR -> {
                "Required motion sensors are unavailable and AR is not providing position."
            }
            session.lifecycleState == RecordingSessionState.ACTIVE && wifiSnapshot.isScanning -> {
                "Recording live Wi-Fi and position samples."
            }
            else -> positionSourceState.status
        }

        return MappingSessionUiState(
            sessionState = session.lifecycleState,
            sessionId = session.sessionId,
            headingDegrees = positionSourceState.preferredSample?.headingDegrees
                ?: motionEstimate.headingDegrees,
            deltaXMeters = positionSourceState.preferredSample?.xMeters
                ?: motionEstimate.movementDelta.deltaXMeters,
            deltaYMeters = positionSourceState.preferredSample?.yMeters
                ?: motionEstimate.movementDelta.deltaYMeters,
            positionSourceType = positionSourceState.preferredSourceType,
            trackingQuality = positionSourceState.trackingQuality,
            trackingStatus = positionSourceState.status,
            sensorSampleCount = motionEstimate.sensorSampleCount,
            trackingState = motionEstimate.trackingState,
            wifiSampleCount = session.wifiSamples.size,
            pathSampleCount = session.pathSamples.size,
            sessionStartedAtEpochMillis = session.sessionStartedAtEpochMillis,
            sessionPausedAtEpochMillis = session.sessionPausedAtEpochMillis,
            lastWifiCaptureAtEpochMillis = session.lastWifiCaptureAtEpochMillis,
            lastPathCaptureAtEpochMillis = session.lastPathCaptureAtEpochMillis,
            statusMessage = statusMessage,
            pathSamples = session.pathSamples,
            wifiSamples = session.wifiSamples
        )
    }

    private fun MotionEstimate.toSensorPositionSample(): LivePositionSample {
        val quality = when (trackingState) {
            MotionTrackingState.TRACKING -> TrackingQuality.LIMITED
            MotionTrackingState.IDLE -> TrackingQuality.UNAVAILABLE
            MotionTrackingState.SENSOR_UNAVAILABLE -> TrackingQuality.UNAVAILABLE
        }
        val status = when (trackingState) {
            MotionTrackingState.TRACKING -> "Sensor-based position tracking is active."
            MotionTrackingState.IDLE -> "Sensor-based position tracking is idle."
            MotionTrackingState.SENSOR_UNAVAILABLE -> "Sensor-based tracking is unavailable."
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
}

private data class SessionRecordingState(
    val lifecycleState: RecordingSessionState = RecordingSessionState.IDLE,
    val sessionId: Long? = null,
    val sessionStartedAtEpochMillis: Long? = null,
    val sessionPausedAtEpochMillis: Long? = null,
    val pathSamples: List<RecordedPathSample> = emptyList(),
    val wifiSamples: List<RecordedWifiSample> = emptyList(),
    val lastRecordedPositionSequence: Long = 0,
    val lastRecordedWifiSnapshotAtEpochMillis: Long = 0,
    val lastSensorSampleCount: Int = 0,
    val lastPositionSourceType: PositionSourceType = PositionSourceType.NONE,
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
