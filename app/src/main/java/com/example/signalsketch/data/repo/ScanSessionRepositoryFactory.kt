package com.example.signalsketch.data.repo

import android.content.Context
import com.example.signalsketch.data.local.DatabaseFactory

object ScanSessionRepositoryFactory {
    @Volatile
    private var repository: ScanSessionRepository? = null

    fun create(context: Context): ScanSessionRepository {
        return repository ?: synchronized(this) {
            repository ?: buildRepository(context.applicationContext).also { repository = it }
        }
    }

    private fun buildRepository(context: Context): ScanSessionRepository {
        val database = DatabaseFactory.create(context)
        return RoomScanSessionRepository(
            database = database,
            scanSessionDao = database.scanSessionDao(),
            wifiSampleDao = database.wifiSampleDao(),
            pathPointDao = database.pathPointDao()
        )
    }
}
