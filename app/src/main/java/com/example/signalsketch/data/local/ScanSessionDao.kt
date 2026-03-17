package com.example.signalsketch.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanSessionDao {
    @Query("SELECT * FROM scan_sessions ORDER BY started_at_epoch_millis DESC")
    fun observeSessions(): Flow<List<ScanSessionEntity>>

    @Transaction
    @Query("SELECT * FROM scan_sessions WHERE session_id = :sessionId")
    fun observeSessionDetail(sessionId: Long): Flow<SessionDetail?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ScanSessionEntity): Long

    @Update
    suspend fun updateSession(session: ScanSessionEntity)

    @Delete
    suspend fun deleteSession(session: ScanSessionEntity)
}
