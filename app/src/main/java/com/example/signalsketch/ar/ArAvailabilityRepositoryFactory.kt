package com.example.signalsketch.ar

import android.content.Context

object ArAvailabilityRepositoryFactory {
    fun create(context: Context): ArAvailabilityRepository {
        val applicationContext = context.applicationContext
        return DefaultArAvailabilityRepository(
            availabilityChecker = ArAvailabilityChecker(applicationContext),
            cameraPermissionManager = CameraPermissionManager(applicationContext)
        )
    }
}
