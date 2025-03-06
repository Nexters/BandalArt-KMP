package com.nexters.bandalart.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class BandalartDatabaseFactory {
    actual fun create(): RoomDatabase.Builder<BandalartDatabase> {
        val dbFilePath = documentDirectory() + "/${BandalartDatabase.DB_NAME}"
        return Room.databaseBuilder<BandalartDatabase>(
            name = dbFilePath
        ).setDriver(BundledSQLiteDriver())
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun documentDirectory(): String {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        return requireNotNull(documentDirectory?.path)
    }
}
