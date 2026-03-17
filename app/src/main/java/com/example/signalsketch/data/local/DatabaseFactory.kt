package com.example.signalsketch.data.local

import android.content.Context
import androidx.room.Room

object DatabaseFactory {
    private const val DATABASE_NAME = "signal_sketch.db"

    fun create(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context = context.applicationContext,
            klass = AppDatabase::class.java,
            name = DATABASE_NAME
        ).build()
    }
}
