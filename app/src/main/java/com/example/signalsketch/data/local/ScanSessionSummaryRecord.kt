package com.example.signalsketch.data.local

import androidx.room.ColumnInfo
import com.example.signalsketch.data.repo.SavedSessionStatus

data class ScanSessionSummaryRecord(
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "started_at_epoch_millis")
    val startedAtEpochMillis: Long,
    @ColumnInfo(name = "ended_at_epoch_millis")
    val endedAtEpochMillis: Long?,
    @ColumnInfo(name = "status")
    val status: SavedSessionStatus,
    @ColumnInfo(name = "notes")
    val notes: String?,
    @ColumnInfo(name = "path_point_count")
    val pathPointCount: Int,
    @ColumnInfo(name = "wifi_sample_count")
    val wifiSampleCount: Int
)
