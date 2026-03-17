package com.example.signalsketch.wifi

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager

class AndroidWifiScanner(
    context: Context
) : WifiScanner {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    @SuppressLint("MissingPermission")
    override fun getConnectedNetwork(): ConnectedWifiNetwork? {
        return runCatching {
            WifiInfoMapper.mapConnectedNetwork(wifiManager.connectionInfo)
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    override fun getVisibleNetworks(): List<VisibleWifiNetwork> {
        return runCatching {
            wifiManager.scanResults
                .orEmpty()
                .map(WifiInfoMapper::mapVisibleNetwork)
                .sortedByDescending { it.rssiDbm }
        }.getOrElse { emptyList() }
    }

    @SuppressLint("MissingPermission")
    override fun startScan(): Boolean {
        return runCatching { wifiManager.startScan() }.getOrDefault(false)
    }
}
