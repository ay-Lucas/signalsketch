package com.example.signalsketch.position

import android.content.Context

object PositionSourceRepositoryFactory {
    @Volatile
    private var instance: PositionSourceRepository? = null

    fun create(context: Context): PositionSourceRepository {
        return instance ?: synchronized(this) {
            instance ?: DefaultPositionSourceRepository().also { instance = it }
        }
    }
}
