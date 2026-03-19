package com.example.signalsketch.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ScanSessionEntity::class,
        WifiSampleEntity::class,
        PathPointEntity::class
    ],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ],
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanSessionDao(): ScanSessionDao
    abstract fun wifiSampleDao(): WifiSampleDao
    abstract fun pathPointDao(): PathPointDao
}
