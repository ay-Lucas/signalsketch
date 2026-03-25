package com.example.signalsketch.data.repo

import androidx.room.withTransaction
import com.example.signalsketch.data.local.AppDatabase
import com.example.signalsketch.data.local.PathPointDao
import com.example.signalsketch.data.local.PathPointEntity
import com.example.signalsketch.data.local.ScanSessionDao
import com.example.signalsketch.data.local.ScanSessionEntity
import com.example.signalsketch.data.local.ScanSessionSummaryRecord
import com.example.signalsketch.data.local.SessionDetail
import com.example.signalsketch.data.local.WifiSampleDao
import com.example.signalsketch.data.local.WifiSampleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomScanSessionRepository(
    private val database: AppDatabase,
    private val scanSessionDao: ScanSessionDao,
    private val wifiSampleDao: WifiSampleDao,
    private val pathPointDao: PathPointDao
) : ScanSessionRepository {
    override fun observeSessions(): Flow<List<SavedSessionSummary>> {
        return scanSessionDao.observeSessions().map { sessions ->
            sessions.map { it.toSummary() }
        }
    }

    override fun observeSessionDetail(sessionId: Long): Flow<SavedSessionDetail?> {
        return scanSessionDao.observeSessionDetail(sessionId).map { detail ->
            detail?.toSavedSessionDetail()
        }
    }

    override suspend fun deleteSession(sessionId: Long) {
        scanSessionDao.deleteSessionById(sessionId)
    }

    override suspend fun saveRecordedSession(session: RecordedSessionPayload): Long {
        return database.withTransaction {
            val sessionId = scanSessionDao.insertSession(
                ScanSessionEntity(
                    name = session.name,
                    startedAtEpochMillis = session.startedAtEpochMillis,
                    endedAtEpochMillis = session.endedAtEpochMillis,
                    status = session.status,
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

            sessionId
        }
    }
}

private fun ScanSessionSummaryRecord.toSummary(): SavedSessionSummary {
    return SavedSessionSummary(
        sessionId = sessionId,
        name = name,
        startedAtEpochMillis = startedAtEpochMillis,
        endedAtEpochMillis = endedAtEpochMillis,
        status = status,
        notes = notes,
        pathPointCount = pathPointCount,
        wifiSampleCount = wifiSampleCount
    )
}

private fun SessionDetail.toSavedSessionDetail(): SavedSessionDetail {
    return SavedSessionDetail(
        summary = SavedSessionSummary(
            sessionId = session.sessionId,
            name = session.name,
            startedAtEpochMillis = session.startedAtEpochMillis,
            endedAtEpochMillis = session.endedAtEpochMillis,
            status = session.status,
            notes = session.notes,
            pathPointCount = pathPoints.size,
            wifiSampleCount = wifiSamples.size
        ),
        pathPoints = pathPoints.map { point ->
            SavedPathPoint(
                pointId = point.pointId,
                xMeters = point.xMeters,
                yMeters = point.yMeters,
                headingDegrees = point.headingDegrees,
                recordedAtEpochMillis = point.recordedAtEpochMillis
            )
        },
        wifiSamples = wifiSamples.map { sample ->
            SavedWifiSample(
                sampleId = sample.sampleId,
                ssid = sample.ssid,
                bssid = sample.bssid,
                rssiDbm = sample.rssiDbm,
                frequencyMhz = sample.frequencyMhz,
                sampledAtEpochMillis = sample.sampledAtEpochMillis,
                xMeters = sample.xMeters,
                yMeters = sample.yMeters,
                headingDegrees = sample.headingDegrees,
                pathPointId = sample.pathPointId
            )
        }
    )
}
