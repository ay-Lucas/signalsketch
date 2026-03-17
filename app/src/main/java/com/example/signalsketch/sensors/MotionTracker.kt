package com.example.signalsketch.sensors

import kotlinx.coroutines.flow.Flow

interface MotionTracker {
    val motionSamples: Flow<MotionSample>

    fun startTracking(): Boolean

    fun stopTracking()
}
