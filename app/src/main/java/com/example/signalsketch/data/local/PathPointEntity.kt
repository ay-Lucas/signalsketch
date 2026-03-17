package com.example.signalsketch.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "path_points",
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
data class PathPointEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "point_id")
    val pointId: Long = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    @ColumnInfo(name = "x_meters")
    val xMeters: Float,
    @ColumnInfo(name = "y_meters")
    val yMeters: Float,
    @ColumnInfo(name = "heading_degrees")
    val headingDegrees: Float? = null,
    @ColumnInfo(name = "recorded_at_epoch_millis")
    val recordedAtEpochMillis: Long
)
