package com.example.signalsketch.ui.scan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.signalsketch.viewmodel.ScanScreenState
import com.example.signalsketch.viewmodel.ScanViewModel
import com.example.signalsketch.wifi.ConnectedWifiNetwork
import com.example.signalsketch.wifi.VisibleWifiNetwork

@Composable
fun LiveScanScreen(viewModel: ScanViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onPermissionRequestResult()
    }

    LiveScanScreen(
        state = uiState,
        onRequestPermission = {
            permissionsLauncher.launch(uiState.permissionStatus.requiredRuntimePermissions.toTypedArray())
        },
        onToggleScanning = viewModel::onToggleScanning,
        onRefresh = viewModel::refresh
    )
}

@Composable
private fun LiveScanScreen(
    state: ScanScreenState,
    onRequestPermission: () -> Unit,
    onToggleScanning: () -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Live Scan",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        item {
            Text(
                text = "Review permissions, check the connected network, and scan for nearby access points.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            PermissionStatusCard(
                state = state,
                onRequestPermission = onRequestPermission
            )
        }
        item {
            ScanControlsRow(
                canScan = state.hasPermission,
                isScanning = state.isScanning,
                onToggleScanning = onToggleScanning,
                onRefresh = onRefresh
            )
        }
        item {
            ConnectedNetworkCard(connectedNetwork = state.connectedNetwork)
        }
        when {
            state.isLoading -> {
                item {
                    LoadingCard()
                }
            }
            state.errorMessage != null -> {
                item {
                    StatusCard(
                        title = "Status",
                        message = state.errorMessage
                    )
                }
            }
            state.visibleNetworks.isEmpty() -> {
                item {
                    StatusCard(
                        title = "No Networks",
                        message = "No visible Wi-Fi networks have been captured yet."
                    )
                }
            }
            else -> {
                item {
                    Text(
                        text = "Visible Networks (${state.visibleNetworks.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(
                    items = state.visibleNetworks,
                    key = { network -> network.bssid }
                ) { network ->
                    VisibleNetworkCard(network = network)
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    state: ScanScreenState,
    onRequestPermission: () -> Unit
) {
    val permissionText = when {
        state.permissionStatus.isGranted -> "Permissions granted"
        state.permissionStatus.requiresLocationServices &&
            !state.permissionStatus.isLocationServicesEnabled -> {
            "Location services are disabled for Wi-Fi scanning"
        }
        state.permissionStatus.missingRuntimePermissions.isNotEmpty() -> {
            "Missing: ${state.permissionStatus.missingRuntimePermissions.joinToString()}"
        }
        else -> "Permissions incomplete"
    }
    val guidanceText = when {
        state.permissionStatus.shouldShowRationale -> {
            "Android denied Wi-Fi-related permissions before. Request them again after explaining that scan results are needed to map signal strength."
        }
        state.permissionStatus.requiresLocationServices &&
            !state.permissionStatus.isLocationServicesEnabled -> {
            "Enable location services in system settings, then refresh this screen. Standard mapping without live scan is still available elsewhere in the app."
        }
        state.permissionStatus.missingRuntimePermissions.isNotEmpty() -> {
            "This screen needs Wi-Fi permissions and, on older Android versions, location-related access to read scan results."
        }
        else -> "This device is ready to read connected Wi-Fi info and visible scan results."
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Permission State",
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = permissionText)
            Text(
                text = guidanceText,
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = onRequestPermission,
                enabled = state.permissionStatus.missingRuntimePermissions.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Request Permission")
            }
        }
    }
}

@Composable
private fun ScanControlsRow(
    canScan: Boolean,
    isScanning: Boolean,
    onToggleScanning: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onToggleScanning,
            enabled = canScan,
            modifier = Modifier.weight(1f)
        ) {
            Text(if (isScanning) "Stop Scanning" else "Start Scanning")
        }
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.weight(1f)
        ) {
            Text("Refresh")
        }
    }
}

@Composable
private fun ConnectedNetworkCard(connectedNetwork: ConnectedWifiNetwork?) {
    val message = connectedNetwork?.let { network ->
        buildString {
            appendLine("SSID: ${network.ssid ?: "Unknown"}")
            appendLine("BSSID: ${network.bssid ?: "Unknown"}")
            appendLine("RSSI: ${network.rssiDbm?.let { "$it dBm" } ?: "Unknown"}")
            appendLine("Frequency: ${network.frequencyMhz?.let { "$it MHz" } ?: "Unknown"}")
            append("Link Speed: ${network.linkSpeedMbps?.let { "$it Mbps" } ?: "Unknown"}")
        }
    } ?: "No connected Wi-Fi network information is available."

    StatusCard(
        title = "Connected Network",
        message = message,
        contentDescription = "Connected Wi-Fi network details. $message"
    )
}

@Composable
private fun VisibleNetworkCard(network: VisibleWifiNetwork) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = buildString {
                    append(network.ssid ?: "Hidden SSID")
                    append(". Signal ")
                    append("${network.rssiDbm} dBm. ")
                    append("BSSID ${network.bssid}.")
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = network.ssid ?: "Hidden SSID",
                style = MaterialTheme.typography.titleSmall
            )
            Text(text = "BSSID: ${network.bssid}")
            Text(text = "RSSI: ${network.rssiDbm} dBm")
            Text(text = "Frequency: ${network.frequencyMhz?.let { "$it MHz" } ?: "Unknown"}")
            Text(text = "Timestamp: ${network.timestampMicros} us")
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Scanning for nearby Wi-Fi networks...",
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    message: String,
    contentDescription: String = "$title. $message"
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                this.contentDescription = contentDescription
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = message)
        }
    }
}
