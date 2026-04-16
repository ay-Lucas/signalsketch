package com.example.signalsketch.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FloorplanRoomBoxDao {
    @Query("SELECT * FROM floorplan_room_boxes WHERE session_id = :sessionId ORDER BY box_id ASC")
    fun observeBoxesForSession(sessionId: Long): Flow<List<FloorplanRoomBoxEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoxes(boxes: List<FloorplanRoomBoxEntity>)
}