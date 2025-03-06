package com.nexters.bandalart.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual class BandalartDatabaseFactory(
    private val context: Context
) {
    actual fun create(): RoomDatabase.Builder<BandalartDatabase> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath(BandalartDatabase.DB_NAME)

        return Room.databaseBuilder(
            context = appContext,
            name = dbFile.absolutePath
        )
    }
}
