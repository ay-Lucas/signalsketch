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
