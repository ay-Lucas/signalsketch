package com.example.signalsketch.wifi

import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

interface WifiRepository : Closeable {
    val permissionStatus: StateFlow<WifiPermissionStatus>

    val scanSnapshot: StateFlow<WifiScanSnapshot>

    fun refreshPermissions()

    fun refreshConnectedNetwork()

    fun refreshScanResults()

    fun startScan(): Boolean

    fun stopScan()
}
