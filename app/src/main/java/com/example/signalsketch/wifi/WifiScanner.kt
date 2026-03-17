package com.example.signalsketch.wifi

interface WifiScanner {
    fun getConnectedNetwork(): ConnectedWifiNetwork?

    fun getVisibleNetworks(): List<VisibleWifiNetwork>

    fun startScan(): Boolean
}
