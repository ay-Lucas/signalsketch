package com.example.signalsketch.data.local

import android.content.Context
import androidx.room.Room

object DatabaseFactory {
    private const val DATABASE_NAME = "signal_sketch.db"
    @Volatile
    private var instance: AppDatabase? = null

    fun create(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context = context.applicationContext,
                klass = AppDatabase::class.java,
                name = DATABASE_NAME
            )
                .enableMultiInstanceInvalidation()
                .build()
                .also { instance = it }
        }
    }
}
