package com.example.signalsketch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.signalsketch.wifi.ConnectedWifiNetwork
import com.example.signalsketch.wifi.VisibleWifiNetwork
import com.example.signalsketch.wifi.WifiPermissionStatus
import com.example.signalsketch.wifi.WifiRepositoryFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScanScreenState(
    val permissionStatus: WifiPermissionStatus,
    val connectedNetwork: ConnectedWifiNetwork? = null,
    val visibleNetworks: List<VisibleWifiNetwork> = emptyList(),
    val isScanning: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val hasPermission: Boolean
        get() = permissionStatus.isGranted

    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && connectedNetwork == null && visibleNetworks.isEmpty()
}

class ScanViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val wifiRepository = WifiRepositoryFactory.create(application)
    private val statusMessages = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ScanScreenState> = combine(
        wifiRepository.permissionStatus,
        wifiRepository.scanSnapshot,
        statusMessages
    ) { permissionStatus, snapshot, statusMessage ->
        val errorMessage = when {
            !permissionStatus.isGranted && permissionStatus.missingRuntimePermissions.isNotEmpty() -> {
                "Wi-Fi permissions are required before scanning can start."
            }
            !permissionStatus.isGranted && permissionStatus.requiresLocationServices -> {
                "Location services must be enabled for Wi-Fi scanning on this Android version."
            }
            snapshot.isScanning && snapshot.visibleNetworks.isEmpty() -> null
            snapshot.visibleNetworks.isEmpty() && snapshot.connectedNetwork == null -> {
                statusMessage ?: "No Wi-Fi data is available yet. Refresh or start scanning to try again."
            }
            else -> statusMessage
        }

        ScanScreenState(
            permissionStatus = permissionStatus,
            connectedNetwork = snapshot.connectedNetwork,
            visibleNetworks = snapshot.visibleNetworks,
            isScanning = snapshot.isScanning,
            isLoading = snapshot.isScanning && snapshot.visibleNetworks.isEmpty(),
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScanScreenState(permissionStatus = wifiRepository.permissionStatus.value)
    )

    init {
        refresh()
    }

    fun onPermissionRequestResult() {
        wifiRepository.refreshPermissions()
        wifiRepository.refreshConnectedNetwork()
        wifiRepository.refreshScanResults()
        statusMessages.value = null
    }

    fun onToggleScanning() {
        if (uiState.value.isScanning) {
            wifiRepository.stopScan()
            statusMessages.value = "Scanning stopped."
            return
        }

        val started = wifiRepository.startScan()
        statusMessages.value = if (started) {
            "Scanning started. Nearby networks will appear here when results arrive."
        } else {
            "Scanning could not start. Check permissions, location services, or device Wi-Fi state."
        }
    }

    fun refresh() {
        viewModelScope.launch {
            wifiRepository.refreshPermissions()
            wifiRepository.refreshConnectedNetwork()
            wifiRepository.refreshScanResults()
            statusMessages.update { currentMessage ->
                currentMessage?.takeIf { uiState.value.isScanning }
            }
        }
    }

    override fun onCleared() {
        wifiRepository.close()
        super.onCleared()
    }
}
