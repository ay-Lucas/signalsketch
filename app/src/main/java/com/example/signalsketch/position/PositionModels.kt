package com.example.signalsketch.position

enum class PositionSourceType {
    NONE,
    SENSORS,
    AR
}

enum class TrackingQuality {
    UNAVAILABLE,
    LIMITED,
    GOOD
}

data class LivePositionSample(
    val sourceType: PositionSourceType,
    val xMeters: Float,
    val yMeters: Float,
    val headingDegrees: Float,
    val trackingQuality: TrackingQuality,
    val status: String,
    val sequence: Long,
    val recordedAtEpochMillis: Long
)

data class PositionSourceState(
    val preferredSourceType: PositionSourceType = PositionSourceType.NONE,
    val preferredSample: LivePositionSample? = null,
    val sensorSample: LivePositionSample? = null,
    val arSample: LivePositionSample? = null,
    val trackingQuality: TrackingQuality = TrackingQuality.UNAVAILABLE,
    val status: String = "No position source active."
)
