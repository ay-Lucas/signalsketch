package com.example.signalsketch.sensors

import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

interface MotionTrackingRepository : Closeable {
    val motionEstimate: StateFlow<MotionEstimate>

    fun startTracking()

    fun stopTracking()
}
