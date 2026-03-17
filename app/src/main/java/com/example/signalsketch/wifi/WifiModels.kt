package com.example.signalsketch.wifi

data class ConnectedWifiNetwork(
    val ssid: String?,
    val bssid: String?,
    val rssiDbm: Int?,
    val linkSpeedMbps: Int?,
    val frequencyMhz: Int?,
    val networkId: Int?
)

data class VisibleWifiNetwork(
    val ssid: String?,
    val bssid: String,
    val capabilities: String,
    val rssiDbm: Int,
    val frequencyMhz: Int?,
    val channelWidthMhz: Int?,
    val is80211mcResponder: Boolean,
    val timestampMicros: Long
)

data class WifiScanSnapshot(
    val connectedNetwork: ConnectedWifiNetwork? = null,
    val visibleNetworks: List<VisibleWifiNetwork> = emptyList(),
    val lastScanStartedAtEpochMillis: Long? = null,
    val lastScanCompletedAtEpochMillis: Long? = null,
    val isScanning: Boolean = false
)

data class WifiPermissionStatus(
    val requiredRuntimePermissions: List<String>,
    val missingRuntimePermissions: List<String>,
    val shouldShowRationale: Boolean,
    val isLocationServicesEnabled: Boolean,
    val requiresLocationServices: Boolean
) {
    val isGranted: Boolean
        get() = missingRuntimePermissions.isEmpty() &&
            (!requiresLocationServices || isLocationServicesEnabled)
}
