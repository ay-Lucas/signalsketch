package com.example.signalsketch.position

import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

interface PositionSourceRepository : Closeable {
    val positionSourceState: StateFlow<PositionSourceState>

    fun updateSensorPosition(sample: LivePositionSample)

    fun updateArPosition(sample: LivePositionSample)

    fun clearArPosition(status: String = "AR position source inactive.")
}
