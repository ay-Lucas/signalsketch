package com.example.signalsketch.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wifi_samples",
    foreignKeys = [
        ForeignKey(
            entity = ScanSessionEntity::class,
            parentColumns = ["session_id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("session_id")]
)
data class WifiSampleEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "sample_id")
    val sampleId: Long = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    @ColumnInfo(name = "ssid")
    val ssid: String?,
    @ColumnInfo(name = "bssid")
    val bssid: String,
    @ColumnInfo(name = "rssi_dbm")
    val rssiDbm: Int,
    @ColumnInfo(name = "frequency_mhz")
    val frequencyMhz: Int?,
    @ColumnInfo(name = "sampled_at_epoch_millis")
    val sampledAtEpochMillis: Long,
    @ColumnInfo(name = "x_meters")
    val xMeters: Float? = null,
    @ColumnInfo(name = "y_meters")
    val yMeters: Float? = null,
    @ColumnInfo(name = "heading_degrees")
    val headingDegrees: Float? = null,
    @ColumnInfo(name = "path_point_id")
    val pathPointId: Long? = null
)
