package com.example.signalsketch.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "floorplan_room_boxes",
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
data class FloorplanRoomBoxEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "box_id")
    val boxId: Long = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    @ColumnInfo(name = "label")
    val label: String,
    @ColumnInfo(name = "center_x_meters")
    val centerXMeters: Float,
    @ColumnInfo(name = "center_y_meters")
    val centerYMeters: Float,
    @ColumnInfo(name = "width_meters")
    val widthMeters: Float,
    @ColumnInfo(name = "height_meters")
    val heightMeters: Float,
    @ColumnInfo(name = "color_argb")
    val colorArgb: Int
)