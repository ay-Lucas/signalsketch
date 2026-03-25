package com.example.signalsketch.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.signalsketch.data.repo.SavedSessionStatus

@Database(
    entities = [
        ScanSessionEntity::class,
        WifiSampleEntity::class,
        PathPointEntity::class
    ],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3)
    ],
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanSessionDao(): ScanSessionDao
    abstract fun wifiSampleDao(): WifiSampleDao
    abstract fun pathPointDao(): PathPointDao
}

class RoomConverters {
    @TypeConverter
    fun savedSessionStatusToString(value: SavedSessionStatus): String = value.name

    @TypeConverter
    fun stringToSavedSessionStatus(value: String): SavedSessionStatus = SavedSessionStatus.valueOf(value)
}
