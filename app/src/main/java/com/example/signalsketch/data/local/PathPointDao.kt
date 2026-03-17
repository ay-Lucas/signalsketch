package com.example.signalsketch.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PathPointDao {
    @Query("SELECT * FROM path_points WHERE session_id = :sessionId ORDER BY recorded_at_epoch_millis ASC")
    fun observePathPointsForSession(sessionId: Long): Flow<List<PathPointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: PathPointEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<PathPointEntity>)
}
