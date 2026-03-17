package com.example.signalsketch.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class SessionDetail(
    @Embedded
    val session: ScanSessionEntity,
    @Relation(
        parentColumn = "session_id",
        entityColumn = "session_id"
    )
    val wifiSamples: List<WifiSampleEntity>,
    @Relation(
        parentColumn = "session_id",
        entityColumn = "session_id"
    )
    val pathPoints: List<PathPointEntity>
)
