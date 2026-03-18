package com.future.ultimate.core.database

import android.content.Context
import androidx.room.Room

object DatabaseFactory {
    fun create(context: Context): FlotaDatabase = Room.databaseBuilder(
        context,
        FlotaDatabase::class.java,
        "future_v20.db",
    ).fallbackToDestructiveMigration().build()
}
