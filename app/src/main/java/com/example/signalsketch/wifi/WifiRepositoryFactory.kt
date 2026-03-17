package com.example.signalsketch.wifi

import android.content.Context

object WifiRepositoryFactory {
    fun create(context: Context): WifiRepository {
        val applicationContext = context.applicationContext
        return DefaultWifiRepository(
            context = applicationContext,
            wifiScanner = AndroidWifiScanner(applicationContext),
            permissionManager = WifiPermissionManager(applicationContext)
        )
    }
}
