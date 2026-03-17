package com.example.signalsketch.wifi

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class WifiPermissionManager(
    private val context: Context
) {
    fun requiredRuntimePermissions(): List<String> {
        return buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            } else {
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    fun missingRuntimePermissions(): List<String> {
        return requiredRuntimePermissions().filterNot { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getPermissionStatus(activity: Activity? = null): WifiPermissionStatus {
        val missingPermissions = missingRuntimePermissions()
        return WifiPermissionStatus(
            requiredRuntimePermissions = requiredRuntimePermissions(),
            missingRuntimePermissions = missingPermissions,
            shouldShowRationale = activity?.let { hostActivity ->
                missingPermissions.any { permission ->
                    ActivityCompat.shouldShowRequestPermissionRationale(hostActivity, permission)
                }
            } ?: false,
            isLocationServicesEnabled = isLocationServicesEnabled(),
            requiresLocationServices = requiresLocationServices()
        )
    }

    fun requiresLocationServices(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    }

    fun isLocationServicesEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF
            ) != Settings.Secure.LOCATION_MODE_OFF
        }
    }
}
