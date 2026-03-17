package com.example.signalsketch.ar

import android.app.Activity
import android.content.Context
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException

class ArAvailabilityChecker(
    private val context: Context
) {
    fun checkSupport(): ArSupportState {
        return when (ArCoreApk.getInstance().checkAvailability(context.applicationContext)) {
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
            ArCoreApk.Availability.SUPPORTED_INSTALLED,
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> ArSupportState.SUPPORTED

            ArCoreApk.Availability.UNKNOWN_CHECKING,
            ArCoreApk.Availability.UNKNOWN_ERROR,
            ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> ArSupportState.CHECKING

            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> ArSupportState.UNSUPPORTED
        }
    }

    fun requestInstall(activity: Activity, userRequestedInstall: Boolean): ArInstallState {
        return try {
            when (ArCoreApk.getInstance().requestInstall(activity, userRequestedInstall)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> ArInstallState.INSTALLING
                ArCoreApk.InstallStatus.INSTALLED -> ArInstallState.READY
            }
        } catch (_: UnavailableUserDeclinedInstallationException) {
            ArInstallState.FAILED
        } catch (_: UnavailableDeviceNotCompatibleException) {
            ArInstallState.FAILED
        }
    }
}
