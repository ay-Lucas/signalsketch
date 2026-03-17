package com.example.signalsketch.ar

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DefaultArAvailabilityRepository(
    private val availabilityChecker: ArAvailabilityChecker,
    private val cameraPermissionManager: CameraPermissionManager
) : ArAvailabilityRepository {
    private var hasRequestedInstall = false

    private val _availabilityState = MutableStateFlow(
        ArAvailabilityState(
            hasCameraPermission = cameraPermissionManager.isCameraPermissionGranted()
        )
    )
    override val availabilityState: StateFlow<ArAvailabilityState> = _availabilityState.asStateFlow()

    init {
        refreshAvailability()
        refreshCameraPermission()
    }

    override fun refreshAvailability() {
        val supportState = availabilityChecker.checkSupport()
        val installState = when (supportState) {
            ArSupportState.SUPPORTED -> {
                if (_availabilityState.value.installState == ArInstallState.NOT_CHECKED) {
                    ArInstallState.REQUIRES_INSTALL
                } else {
                    _availabilityState.value.installState
                }
            }
            ArSupportState.CHECKING -> ArInstallState.NOT_CHECKED
            ArSupportState.UNSUPPORTED -> ArInstallState.FAILED
        }
        updateState(
            supportState = supportState,
            installState = installState
        )
    }

    override fun refreshCameraPermission() {
        updateState(
            hasCameraPermission = cameraPermissionManager.isCameraPermissionGranted()
        )
    }

    override fun requestInstall(activity: Activity) {
        val result = availabilityChecker.requestInstall(
            activity = activity,
            userRequestedInstall = !hasRequestedInstall
        )
        hasRequestedInstall = true
        updateState(installState = result)
        if (result == ArInstallState.READY || result == ArInstallState.INSTALLING) {
            refreshAvailability()
        }
    }

    override fun markInstallFlowRetried() {
        hasRequestedInstall = true
    }

    private fun updateState(
        supportState: ArSupportState = _availabilityState.value.supportState,
        installState: ArInstallState = _availabilityState.value.installState,
        hasCameraPermission: Boolean = _availabilityState.value.hasCameraPermission
    ) {
        val canStartAr = supportState == ArSupportState.SUPPORTED &&
            installState == ArInstallState.READY &&
            hasCameraPermission
        val statusMessage = when {
            supportState == ArSupportState.CHECKING -> "Checking ARCore availability."
            supportState == ArSupportState.UNSUPPORTED -> "This device does not support ARCore."
            installState == ArInstallState.REQUIRES_INSTALL -> "Google Play Services for AR is not ready."
            installState == ArInstallState.INSTALLING -> "ARCore install or update has been requested."
            installState == ArInstallState.FAILED -> "ARCore install or update could not be completed."
            !hasCameraPermission -> "Camera permission is required before AR can start."
            else -> "AR is ready to start."
        }

        _availabilityState.value = ArAvailabilityState(
            supportState = supportState,
            installState = installState,
            hasCameraPermission = hasCameraPermission,
            canStartAr = canStartAr,
            statusMessage = statusMessage
        )
    }
}
