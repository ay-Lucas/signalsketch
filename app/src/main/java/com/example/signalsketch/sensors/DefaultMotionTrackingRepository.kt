package com.example.signalsketch.sensors

import android.hardware.Sensor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class DefaultMotionTrackingRepository(
    private val motionTracker: MotionTracker,
    private val externalScope: CoroutineScope? = null
) : MotionTrackingRepository {
    private companion object {
        const val NOISE_THRESHOLD = 0.35f
        const val DISTANCE_SCALE = 0.015f
        const val NANOSECONDS_PER_SECOND = 1_000_000_000f
        const val RADIANS_TO_DEGREES = 57.29578f
        const val FULL_CIRCLE_DEGREES = 360f
    }

    private val repositoryScope = externalScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val ownsScope = externalScope == null

    private val _motionEstimate = MutableStateFlow(MotionEstimate())
    override val motionEstimate: StateFlow<MotionEstimate> = _motionEstimate.asStateFlow()

    private var trackingJob: Job? = null
    private var lastGyroscopeTimestampNanos: Long? = null
    private var lastAccelerometerTimestampNanos: Long? = null
    private val gravity = FloatArray(3)

    override fun startTracking() {
        if (!motionTracker.startTracking()) {
            _motionEstimate.value = _motionEstimate.value.copy(
                trackingState = MotionTrackingState.SENSOR_UNAVAILABLE
            )
            return
        }
        if (trackingJob != null) return

        resetEstimate(MotionTrackingState.TRACKING)
        trackingJob = repositoryScope.launch {
            motionTracker.motionSamples.collect { sample ->
                when (sample.sensorType) {
                    Sensor.TYPE_GYROSCOPE -> handleGyroscopeSample(sample)
                    Sensor.TYPE_ACCELEROMETER -> handleAccelerometerSample(sample)
                }
            }
        }
    }

    override fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        motionTracker.stopTracking()
        lastGyroscopeTimestampNanos = null
        lastAccelerometerTimestampNanos = null
        gravity.fill(0f)
        _motionEstimate.value = _motionEstimate.value.copy(
            trackingState = MotionTrackingState.IDLE
        )
    }

    override fun close() {
        stopTracking()
        if (ownsScope) {
            repositoryScope.cancel()
        }
    }

    private fun handleGyroscopeSample(sample: MotionSample) {
        val previousTimestamp = lastGyroscopeTimestampNanos
        lastGyroscopeTimestampNanos = sample.timestampNanos
        if (previousTimestamp == null || sample.values.size < 3) {
            incrementSampleCount()
            return
        }

        val deltaSeconds = (sample.timestampNanos - previousTimestamp) / NANOSECONDS_PER_SECOND
        val headingDeltaDegrees = sample.values[2] * deltaSeconds * RADIANS_TO_DEGREES
        val nextHeading = normalizeHeading(_motionEstimate.value.headingDegrees + headingDeltaDegrees)
        _motionEstimate.value = _motionEstimate.value.copy(
            headingDegrees = nextHeading,
            sensorSampleCount = _motionEstimate.value.sensorSampleCount + 1
        )
    }

    private fun handleAccelerometerSample(sample: MotionSample) {
        val previousTimestamp = lastAccelerometerTimestampNanos
        lastAccelerometerTimestampNanos = sample.timestampNanos
        if (sample.values.size < 3) {
            incrementSampleCount()
            return
        }

        gravity[0] = gravity[0] * 0.8f + sample.values[0] * 0.2f
        gravity[1] = gravity[1] * 0.8f + sample.values[1] * 0.2f
        gravity[2] = gravity[2] * 0.8f + sample.values[2] * 0.2f

        val linearX = sample.values[0] - gravity[0]
        val linearY = sample.values[1] - gravity[1]
        val linearMagnitude = sqrt((linearX * linearX + linearY * linearY).toDouble()).toFloat()
        val deltaSeconds = if (previousTimestamp == null) {
            0f
        } else {
            (sample.timestampNanos - previousTimestamp) / NANOSECONDS_PER_SECOND
        }

        val distanceStep = ((linearMagnitude - NOISE_THRESHOLD).coerceAtLeast(0f)) *
            deltaSeconds *
            DISTANCE_SCALE
        val headingRadians = Math.toRadians(_motionEstimate.value.headingDegrees.toDouble())
        val deltaX = (distanceStep * cos(headingRadians)).toFloat()
        val deltaY = (distanceStep * sin(headingRadians)).toFloat()

        _motionEstimate.value = _motionEstimate.value.copy(
            movementDelta = MovementDelta(
                deltaXMeters = _motionEstimate.value.movementDelta.deltaXMeters + deltaX,
                deltaYMeters = _motionEstimate.value.movementDelta.deltaYMeters + deltaY
            ),
            sensorSampleCount = _motionEstimate.value.sensorSampleCount + 1
        )
    }

    private fun incrementSampleCount() {
        _motionEstimate.value = _motionEstimate.value.copy(
            sensorSampleCount = _motionEstimate.value.sensorSampleCount + 1
        )
    }

    private fun resetEstimate(trackingState: MotionTrackingState) {
        lastGyroscopeTimestampNanos = null
        lastAccelerometerTimestampNanos = null
        gravity.fill(0f)
        _motionEstimate.value = MotionEstimate(trackingState = trackingState)
    }

    private fun normalizeHeading(headingDegrees: Float): Float {
        val normalized = headingDegrees % FULL_CIRCLE_DEGREES
        return if (normalized < 0f) normalized + FULL_CIRCLE_DEGREES else normalized
    }
}
