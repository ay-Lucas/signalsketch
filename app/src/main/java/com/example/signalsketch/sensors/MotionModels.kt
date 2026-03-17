package com.example.signalsketch.sensors

data class MotionSample(
    val sensorType: Int,
    val values: List<Float>,
    val timestampNanos: Long
)

data class MovementDelta(
    val deltaXMeters: Float = 0f,
    val deltaYMeters: Float = 0f
)

enum class MotionTrackingState {
    IDLE,
    TRACKING,
    SENSOR_UNAVAILABLE
}

data class MotionEstimate(
    val headingDegrees: Float = 0f,
    val movementDelta: MovementDelta = MovementDelta(),
    val sensorSampleCount: Int = 0,
    val trackingState: MotionTrackingState = MotionTrackingState.IDLE
)
