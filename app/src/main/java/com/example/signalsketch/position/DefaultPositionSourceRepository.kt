package com.example.signalsketch.position

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DefaultPositionSourceRepository : PositionSourceRepository {
    private val _positionSourceState = MutableStateFlow(PositionSourceState())
    override val positionSourceState: StateFlow<PositionSourceState> = _positionSourceState.asStateFlow()

    override fun updateSensorPosition(sample: LivePositionSample) {
        updateState(
            sensorSample = sample,
            status = resolveStatus(arSample = _positionSourceState.value.arSample, sensorSample = sample)
        )
    }

    override fun updateArPosition(sample: LivePositionSample) {
        updateState(
            arSample = sample,
            status = resolveStatus(arSample = sample, sensorSample = _positionSourceState.value.sensorSample)
        )
    }

    override fun clearArPosition(status: String) {
        updateState(
            arSample = null,
            status = status
        )
    }

    override fun close() = Unit

    private fun updateState(
        sensorSample: LivePositionSample? = _positionSourceState.value.sensorSample,
        arSample: LivePositionSample? = _positionSourceState.value.arSample,
        status: String
    ) {
        val preferredSample = when {
            arSample?.trackingQuality == TrackingQuality.GOOD -> arSample
            arSample?.trackingQuality == TrackingQuality.LIMITED && sensorSample == null -> arSample
            sensorSample != null -> sensorSample
            arSample != null -> arSample
            else -> null
        }

        _positionSourceState.value = PositionSourceState(
            preferredSourceType = preferredSample?.sourceType ?: PositionSourceType.NONE,
            preferredSample = preferredSample,
            sensorSample = sensorSample,
            arSample = arSample,
            trackingQuality = preferredSample?.trackingQuality ?: TrackingQuality.UNAVAILABLE,
            status = status
        )
    }

    private fun resolveStatus(
        arSample: LivePositionSample?,
        sensorSample: LivePositionSample?
    ): String {
        return when {
            arSample?.trackingQuality == TrackingQuality.GOOD -> arSample.status
            arSample?.trackingQuality == TrackingQuality.LIMITED && sensorSample == null -> arSample.status
            sensorSample != null -> sensorSample.status
            arSample != null -> arSample.status
            else -> "No position source active."
        }
    }
}
