package com.example.signalsketch.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanSessionDao {
    @Query(
        """
        SELECT
            scan_sessions.*,
            (SELECT COUNT(*) FROM path_points WHERE path_points.session_id = scan_sessions.session_id) AS path_point_count,
            (SELECT COUNT(*) FROM wifi_samples WHERE wifi_samples.session_id = scan_sessions.session_id) AS wifi_sample_count
        FROM scan_sessions
        ORDER BY started_at_epoch_millis DESC
        """
    )
    fun observeSessions(): Flow<List<ScanSessionSummaryRecord>>

    @Transaction
    @Query("SELECT * FROM scan_sessions WHERE session_id = :sessionId")
    fun observeSessionDetail(sessionId: Long): Flow<SessionDetail?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ScanSessionEntity): Long

    @Update
    suspend fun updateSession(session: ScanSessionEntity)

    @Query("DELETE FROM scan_sessions WHERE session_id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)
}
