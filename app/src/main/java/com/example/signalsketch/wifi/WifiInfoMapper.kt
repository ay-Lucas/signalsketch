package com.example.signalsketch.wifi

import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.os.Build

object WifiInfoMapper {
    fun mapConnectedNetwork(info: WifiInfo?): ConnectedWifiNetwork? {
        if (info == null) return null

        return ConnectedWifiNetwork(
            ssid = info.ssid.normalizeSsid(),
            bssid = info.bssid,
            rssiDbm = info.rssi.takeIf { it > -127 },
            linkSpeedMbps = info.linkSpeed.takeIf { it > 0 },
            frequencyMhz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                info.frequency.takeIf { it > 0 }
            } else {
                null
            },
            networkId = info.networkId.takeIf { it >= 0 }
        )
    }

    fun mapVisibleNetwork(result: ScanResult): VisibleWifiNetwork {
        return VisibleWifiNetwork(
            ssid = result.SSID.ifBlank { null },
            bssid = result.BSSID,
            capabilities = result.capabilities.orEmpty(),
            rssiDbm = result.level,
            frequencyMhz = result.frequency.takeIf { it > 0 },
            channelWidthMhz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                result.channelWidth.takeIf { it >= 0 }
            } else {
                null
            },
            is80211mcResponder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                result.is80211mcResponder
            } else {
                false
            },
            timestampMicros = result.timestamp
        )
    }

    private fun String?.normalizeSsid(): String? {
        val value = this?.removeSurrounding("\"")?.takeUnless { it == "<unknown ssid>" }
        return value?.ifBlank { null }
    }
}
