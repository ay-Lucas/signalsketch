package com.example.signalsketch.data.repo

import kotlinx.coroutines.flow.Flow

interface ScanSessionRepository {
    fun observeSessions(): Flow<List<SavedSessionSummary>>

    fun observeSessionDetail(sessionId: Long): Flow<SavedSessionDetail?>

    suspend fun deleteSession(sessionId: Long)

    suspend fun saveRecordedSession(session: RecordedSessionPayload): Long
}
