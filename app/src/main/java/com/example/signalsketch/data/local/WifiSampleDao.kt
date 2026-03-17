package com.example.signalsketch.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WifiSampleDao {
    @Query("SELECT * FROM wifi_samples WHERE session_id = :sessionId ORDER BY sampled_at_epoch_millis ASC")
    fun observeSamplesForSession(sessionId: Long): Flow<List<WifiSampleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: WifiSampleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSamples(samples: List<WifiSampleEntity>)
}
