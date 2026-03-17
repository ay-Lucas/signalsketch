package com.example.signalsketch.data.repo

import com.example.signalsketch.data.local.PathPointEntity
import com.example.signalsketch.data.local.ScanSessionEntity
import com.example.signalsketch.data.local.SessionDetail
import com.example.signalsketch.data.local.WifiSampleEntity
import kotlinx.coroutines.flow.Flow

interface ScanSessionRepository {
    fun observeSessions(): Flow<List<ScanSessionEntity>>

    fun observeSessionDetail(sessionId: Long): Flow<SessionDetail?>

    fun observeWifiSamples(sessionId: Long): Flow<List<WifiSampleEntity>>

    fun observePathPoints(sessionId: Long): Flow<List<PathPointEntity>>

    suspend fun createSession(session: ScanSessionEntity): Long

    suspend fun updateSession(session: ScanSessionEntity)

    suspend fun deleteSession(session: ScanSessionEntity)

    suspend fun addWifiSample(sample: WifiSampleEntity): Long

    suspend fun addWifiSamples(samples: List<WifiSampleEntity>)

    suspend fun addPathPoint(point: PathPointEntity): Long

    suspend fun addPathPoints(points: List<PathPointEntity>)
}
