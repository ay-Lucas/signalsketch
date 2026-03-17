package com.example.signalsketch.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DefaultWifiRepository(
    private val context: Context,
    private val wifiScanner: WifiScanner,
    private val permissionManager: WifiPermissionManager,
    private val externalScope: CoroutineScope? = null
) : WifiRepository {
    private companion object {
        const val SCAN_LOOP_INTERVAL_MILLIS = 5_000L
    }

    private val repositoryScope = externalScope ?: CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ownsScope = externalScope == null

    private val _permissionStatus = MutableStateFlow(permissionManager.getPermissionStatus())
    override val permissionStatus: StateFlow<WifiPermissionStatus> = _permissionStatus.asStateFlow()

    private val _scanSnapshot = MutableStateFlow(WifiScanSnapshot())
    override val scanSnapshot: StateFlow<WifiScanSnapshot> = _scanSnapshot.asStateFlow()

    private var receiverRegistered = false
    private var scanLoopJob: Job? = null

    private val scanResultsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val scanCompleted = intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false) ?: false
            _scanSnapshot.value = _scanSnapshot.value.copy(
                isScanning = false,
                lastScanCompletedAtEpochMillis = System.currentTimeMillis()
            )
            if (scanCompleted) {
                refreshConnectedNetwork()
                refreshScanResults()
            }
        }
    }

    init {
        registerReceiver()
        refreshPermissions()
        refreshConnectedNetwork()
        refreshScanResults()
    }

    override fun refreshPermissions() {
        _permissionStatus.value = permissionManager.getPermissionStatus()
    }

    override fun refreshConnectedNetwork() {
        if (!_permissionStatus.value.isGranted) {
            _scanSnapshot.value = _scanSnapshot.value.copy(connectedNetwork = null)
            return
        }
        _scanSnapshot.value = _scanSnapshot.value.copy(
            connectedNetwork = wifiScanner.getConnectedNetwork()
        )
    }

    override fun refreshScanResults() {
        if (!_permissionStatus.value.isGranted) {
            _scanSnapshot.value = _scanSnapshot.value.copy(
                visibleNetworks = emptyList(),
                isScanning = false,
                lastScanCompletedAtEpochMillis = System.currentTimeMillis()
            )
            return
        }
        _scanSnapshot.value = _scanSnapshot.value.copy(
            visibleNetworks = wifiScanner.getVisibleNetworks(),
            lastScanCompletedAtEpochMillis = System.currentTimeMillis(),
            isScanning = false
        )
    }

    override fun startScan(): Boolean {
        refreshPermissions()
        if (!_permissionStatus.value.isGranted) {
            return false
        }
        if (scanLoopJob?.isActive == true) {
            return true
        }

        val started = triggerSingleScan()
        if (!started) {
            return false
        }

        scanLoopJob = repositoryScope.launch {
            while (true) {
                delay(SCAN_LOOP_INTERVAL_MILLIS)
                refreshPermissions()
                if (!_permissionStatus.value.isGranted) {
                    _scanSnapshot.value = _scanSnapshot.value.copy(isScanning = false)
                    break
                }
                triggerSingleScan()
            }
        }
        return true
    }

    override fun stopScan() {
        scanLoopJob?.cancel()
        scanLoopJob = null
        _scanSnapshot.value = _scanSnapshot.value.copy(isScanning = false)
    }

    override fun close() {
        scanLoopJob?.cancel()
        scanLoopJob = null
        if (receiverRegistered) {
            context.unregisterReceiver(scanResultsReceiver)
            receiverRegistered = false
        }
        if (ownsScope) {
            repositoryScope.coroutineContext.cancel()
        }
    }

    private fun registerReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        ContextCompat.registerReceiver(
            context,
            scanResultsReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiverRegistered = true
        repositoryScope.launch {
            refreshPermissions()
        }
    }

    private fun triggerSingleScan(): Boolean {
        _scanSnapshot.value = _scanSnapshot.value.copy(
            isScanning = true,
            lastScanStartedAtEpochMillis = System.currentTimeMillis()
        )

        val started = wifiScanner.startScan()
        if (!started) {
            _scanSnapshot.value = _scanSnapshot.value.copy(isScanning = false)
        }
        return started
    }
}
