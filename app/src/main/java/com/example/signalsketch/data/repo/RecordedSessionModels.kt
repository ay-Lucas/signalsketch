package com.example.signalsketch.data.repo

enum class SavedSessionStatus {
    PAUSED,
    COMPLETED
}

data class RecordedSessionPayload(
    val name: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
    val status: SavedSessionStatus,
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

data class SavedPathPoint(
    val pointId: Long,
    val xMeters: Float,
    val yMeters: Float,
    val headingDegrees: Float?,
    val recordedAtEpochMillis: Long
)

data class SavedWifiSample(
    val sampleId: Long,
    val ssid: String?,
    val bssid: String,
    val rssiDbm: Int,
    val frequencyMhz: Int?,
    val sampledAtEpochMillis: Long,
    val xMeters: Float?,
    val yMeters: Float?,
    val headingDegrees: Float?,
    val pathPointId: Long?
)

data class SavedSessionSummary(
    val sessionId: Long,
    val name: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
    val status: SavedSessionStatus,
    val notes: String?,
    val pathPointCount: Int,
    val wifiSampleCount: Int
)

data class SavedSessionDetail(
    val summary: SavedSessionSummary,
    val pathPoints: List<SavedPathPoint>,
    val wifiSamples: List<SavedWifiSample>
)
