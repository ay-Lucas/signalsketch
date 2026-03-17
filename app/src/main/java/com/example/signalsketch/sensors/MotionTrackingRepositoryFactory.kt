package com.example.signalsketch.sensors

import android.content.Context

object MotionTrackingRepositoryFactory {
    fun create(context: Context): MotionTrackingRepository {
        val applicationContext = context.applicationContext
        return DefaultMotionTrackingRepository(
            motionTracker = AndroidMotionTracker(applicationContext)
        )
    }
}
