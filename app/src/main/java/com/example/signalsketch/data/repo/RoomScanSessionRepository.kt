package com.example.signalsketch.data.repo

import com.example.signalsketch.data.local.PathPointDao
import com.example.signalsketch.data.local.PathPointEntity
import com.example.signalsketch.data.local.ScanSessionDao
import com.example.signalsketch.data.local.ScanSessionEntity
import com.example.signalsketch.data.local.SessionDetail
import com.example.signalsketch.data.local.WifiSampleDao
import com.example.signalsketch.data.local.WifiSampleEntity
import kotlinx.coroutines.flow.Flow

class RoomScanSessionRepository(
    private val scanSessionDao: ScanSessionDao,
    private val wifiSampleDao: WifiSampleDao,
    private val pathPointDao: PathPointDao
) : ScanSessionRepository {
    override fun observeSessions(): Flow<List<ScanSessionEntity>> = scanSessionDao.observeSessions()

    override fun observeSessionDetail(sessionId: Long): Flow<SessionDetail?> {
        return scanSessionDao.observeSessionDetail(sessionId)
    }

    override fun observeWifiSamples(sessionId: Long): Flow<List<WifiSampleEntity>> {
        return wifiSampleDao.observeSamplesForSession(sessionId)
    }

    override fun observePathPoints(sessionId: Long): Flow<List<PathPointEntity>> {
        return pathPointDao.observePathPointsForSession(sessionId)
    }

    override suspend fun createSession(session: ScanSessionEntity): Long {
        return scanSessionDao.insertSession(session)
    }

    override suspend fun updateSession(session: ScanSessionEntity) {
        scanSessionDao.updateSession(session)
    }

    override suspend fun deleteSession(session: ScanSessionEntity) {
        scanSessionDao.deleteSession(session)
    }

    override suspend fun addWifiSample(sample: WifiSampleEntity): Long {
        return wifiSampleDao.insertSample(sample)
    }

    override suspend fun addWifiSamples(samples: List<WifiSampleEntity>) {
        wifiSampleDao.insertSamples(samples)
    }

    override suspend fun addPathPoint(point: PathPointEntity): Long {
        return pathPointDao.insertPoint(point)
    }

    override suspend fun addPathPoints(points: List<PathPointEntity>) {
        pathPointDao.insertPoints(points)
    }
}
