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

    override suspend fun saveRecordedSession(session: RecordedSessionPayload): Long {
        val sessionId = scanSessionDao.insertSession(
            ScanSessionEntity(
                name = session.name,
                startedAtEpochMillis = session.startedAtEpochMillis,
                endedAtEpochMillis = session.endedAtEpochMillis,
                notes = session.notes
            )
        )

        val pathPointIds = if (session.pathPoints.isNotEmpty()) {
            pathPointDao.insertPoints(
                session.pathPoints.map { point ->
                    PathPointEntity(
                        sessionId = sessionId,
                        xMeters = point.xMeters,
                        yMeters = point.yMeters,
                        headingDegrees = point.headingDegrees,
                        recordedAtEpochMillis = point.recordedAtEpochMillis
                    )
                }
            )
        } else {
            emptyList()
        }

        if (session.wifiSamples.isNotEmpty()) {
            wifiSampleDao.insertSamples(
                session.wifiSamples.map { sample ->
                    WifiSampleEntity(
                        sessionId = sessionId,
                        ssid = sample.ssid,
                        bssid = sample.bssid,
                        rssiDbm = sample.rssiDbm,
                        frequencyMhz = sample.frequencyMhz,
                        sampledAtEpochMillis = sample.sampledAtEpochMillis,
                        xMeters = sample.xMeters,
                        yMeters = sample.yMeters,
                        headingDegrees = sample.headingDegrees,
                        pathPointId = sample.pathPointIndex
                            ?.takeIf { it in pathPointIds.indices }
                            ?.let(pathPointIds::get)
                    )
                }
            )
        }

        return sessionId
    }
}
