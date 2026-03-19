package com.example.signalsketch.ar

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
    val lastErrorMessage: String? = null
)
