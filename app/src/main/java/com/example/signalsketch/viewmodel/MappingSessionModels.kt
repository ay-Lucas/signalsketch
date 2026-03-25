package com.example.signalsketch.viewmodel

import com.example.signalsketch.position.PositionSourceType
import com.example.signalsketch.position.TrackingQuality
import com.example.signalsketch.sensors.MotionTrackingState

enum class RecordingSessionState {
    IDLE,
    ACTIVE,
    PAUSED
}

data class RecordedPathSample(
    val index: Int,
    val xMeters: Float,
    val yMeters: Float,
    val headingDegrees: Float,
    val sensorSampleCount: Int,
    val recordedAtEpochMillis: Long
)

data class RecordedWifiSample(
    val bssid: String,
    val ssid: String?,
    val rssiDbm: Int,
    val frequencyMhz: Int?,
    val timestampMicros: Long,
    val xMeters: Float,
    val yMeters: Float,
    val headingDegrees: Float,
    val pathSampleIndex: Int?,
    val recordedAtEpochMillis: Long
)

data class MappingSessionUiState(
    val sessionState: RecordingSessionState = RecordingSessionState.IDLE,
    val sessionId: Long? = null,
    val headingDegrees: Float = 0f,
    val deltaXMeters: Float = 0f,
    val deltaYMeters: Float = 0f,
    val positionSourceType: PositionSourceType = PositionSourceType.NONE,
    val trackingQuality: TrackingQuality = TrackingQuality.UNAVAILABLE,
    val trackingStatus: String = "No position source active.",
    val sensorSampleCount: Int = 0,
    val trackingState: MotionTrackingState = MotionTrackingState.IDLE,
    val wifiSampleCount: Int = 0,
    val pathSampleCount: Int = 0,
    val sessionStartedAtEpochMillis: Long? = null,
    val sessionPausedAtEpochMillis: Long? = null,
    val lastWifiCaptureAtEpochMillis: Long? = null,
    val lastPathCaptureAtEpochMillis: Long? = null,
    val statusMessage: String? = null,
    val pathSamples: List<RecordedPathSample> = emptyList(),
    val wifiSamples: List<RecordedWifiSample> = emptyList()
) {
    val canStart: Boolean
        get() = sessionState == RecordingSessionState.IDLE

    val canPause: Boolean
        get() = sessionState == RecordingSessionState.ACTIVE

    val canResume: Boolean
        get() = sessionState == RecordingSessionState.PAUSED

    val canSave: Boolean
        get() = sessionState != RecordingSessionState.IDLE && (wifiSampleCount > 0 || pathSampleCount > 0)

    val canReset: Boolean
        get() = sessionState != RecordingSessionState.IDLE || wifiSampleCount > 0 || pathSampleCount > 0
}
