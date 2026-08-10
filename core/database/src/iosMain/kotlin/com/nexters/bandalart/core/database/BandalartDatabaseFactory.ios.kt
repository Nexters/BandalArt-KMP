/*
 * Copyright 2025 easyhooon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        val fileManager = NSFileManager.defaultManager
        val sharedDirectory = sharedDirectory(fileManager)
        migrateLegacyDatabase(fileManager, documentDirectory(fileManager), sharedDirectory)
        return databaseBuilder("$sharedDirectory/${BandalartDatabase.DB_NAME}")
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun documentDirectory(fileManager: NSFileManager): String {
        val documentDirectory =
            fileManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            )
        return requireNotNull(documentDirectory?.path)
    }
}

fun openExistingSharedBandalartDatabase(): BandalartDatabase? {
    val fileManager = NSFileManager.defaultManager
    val databasePath = "${sharedDirectory(fileManager)}/${BandalartDatabase.DB_NAME}"
    if (!fileManager.fileExistsAtPath(databasePath)) return null
    return databaseBuilder(databasePath).build()
}

private fun databaseBuilder(path: String): RoomDatabase.Builder<BandalartDatabase> =
    Room
        .databaseBuilder<BandalartDatabase>(name = path)
        .setDriver(BundledSQLiteDriver())

@OptIn(ExperimentalForeignApi::class)
private fun sharedDirectory(fileManager: NSFileManager): String =
    requireNotNull(
        fileManager.containerURLForSecurityApplicationGroupIdentifier(IOS_APP_GROUP_IDENTIFIER)?.path,
    ) { "Missing App Group container: $IOS_APP_GROUP_IDENTIFIER" }

@OptIn(ExperimentalForeignApi::class)
private fun migrateLegacyDatabase(
    fileManager: NSFileManager,
    sourceDirectory: String,
    destinationDirectory: String,
) {
    val fileNames =
        listOf(
            BandalartDatabase.DB_NAME,
            "${BandalartDatabase.DB_NAME}-wal",
            "${BandalartDatabase.DB_NAME}-shm",
        )
    val sourceFiles = fileNames.filterTo(mutableSetOf()) { fileManager.fileExistsAtPath("$sourceDirectory/$it") }
    val destinationFiles = fileNames.filterTo(mutableSetOf()) { fileManager.fileExistsAtPath("$destinationDirectory/$it") }

    DatabaseFileMigrationPolicy
        .plan(
            baseFileName = BandalartDatabase.DB_NAME,
            sourceFiles = sourceFiles,
            destinationFiles = destinationFiles,
        ).forEach { move ->
            check(
                fileManager.moveItemAtPath(
                    srcPath = "$sourceDirectory/${move.fileName}",
                    toPath = "$destinationDirectory/${move.fileName}",
                    error = null,
                ),
            ) { "Failed to migrate ${move.fileName} to the App Group container" }
        }
}

private const val IOS_APP_GROUP_IDENTIFIER = "group.com.nexters.bandalart"
