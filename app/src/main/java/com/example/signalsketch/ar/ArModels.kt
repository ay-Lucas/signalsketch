package com.example.signalsketch.ar

import com.example.signalsketch.position.TrackingQuality

enum class ArSupportState {
    CHECKING,
    SUPPORTED,
    UNSUPPORTED
}

enum class ArInstallState {
    NOT_CHECKED,
    READY,
    REQUIRES_INSTALL,
    INSTALLING,
    FAILED
}

data class ArAvailabilityState(
    val supportState: ArSupportState = ArSupportState.CHECKING,
    val installState: ArInstallState = ArInstallState.NOT_CHECKED,
    val hasCameraPermission: Boolean = false,
    val canStartAr: Boolean = false,
    val statusMessage: String = "Checking AR availability."
)

enum class ArSessionLifecycleState {
    IDLE,
    CREATED,
    RESUMED,
    PAUSED,
    FAILED
}

data class ArSessionState(
    val lifecycleState: ArSessionLifecycleState = ArSessionLifecycleState.IDLE,
    val hasDetectedHorizontalPlane: Boolean = false,
    val anchorCount: Int = 0,
    val xMeters: Float? = null,
    val yMeters: Float? = null,
    val headingDegrees: Float? = null,
    val trackingQuality: TrackingQuality = TrackingQuality.UNAVAILABLE,
    val trackingStatus: String = "AR session inactive.",
    val lastErrorMessage: String? = null
)
