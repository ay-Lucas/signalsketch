package com.example.signalsketch.data.repo

data class RecordedSessionPayload(
    val name: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val notes: String? = null,
    val pathPoints: List<RecordedPathPointPayload> = emptyList(),
    val wifiSamples: List<RecordedWifiSamplePayload> = emptyList()
)

data class RecordedPathPointPayload(
    val xMeters: Float,
    val yMeters: Float,
    val headingDegrees: Float?,
    val recordedAtEpochMillis: Long
)

data class RecordedWifiSamplePayload(
    val ssid: String?,
    val bssid: String,
    val rssiDbm: Int,
    val frequencyMhz: Int?,
    val sampledAtEpochMillis: Long,
    val xMeters: Float?,
    val yMeters: Float?,
    val headingDegrees: Float?,
    val pathPointIndex: Int? = null
)
