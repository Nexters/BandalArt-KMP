/*
 * Copyright 2026 easyhooon
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

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

@ExtendWith(RobolectricExtension::class)
@Config(sdk = [35])
class BandalartDatabaseMigrationTest {
    private lateinit var context: Context
    private val databaseName = "bandalart-v1-to-v2-migration-test"

    @BeforeEach
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @AfterEach
    fun cleanUp() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun versionOneRowsArePreservedWithDailyResetDisabled() =
        runTest {
            context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { database ->
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bandalarts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                        `mainColor` TEXT NOT NULL,
                        `subColor` TEXT NOT NULL,
                        `profileEmoji` TEXT,
                        `title` TEXT,
                        `description` TEXT,
                        `dueDate` TEXT,
                        `isCompleted` INTEGER NOT NULL,
                        `completionRatio` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bandalart_cells` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                        `bandalartId` INTEGER NOT NULL,
                        `title` TEXT,
                        `description` TEXT,
                        `dueDate` TEXT,
                        `isCompleted` INTEGER NOT NULL,
                        `parentId` INTEGER,
                        FOREIGN KEY(`bandalartId`) REFERENCES `bandalarts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_bandalart_cells_parentId` ON `bandalart_cells` (`parentId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_bandalart_cells_bandalartId` ON `bandalart_cells` (`bandalartId`)")
                database.execSQL(
                    "INSERT INTO bandalarts VALUES (1, '#3FFFBA', '#111827', '🎯', '기존 목표', '설명', NULL, 1, 100)",
                )
                database.execSQL("PRAGMA user_version = 1")
            }

            val database =
                Room
                    .databaseBuilder(context, BandalartDatabase::class.java, databaseName)
                    .addBandalartMigrations()
                    .build()
            try {
                val migrated = database.bandalartDao.getBandalart(1L)

                assertEquals("기존 목표", migrated.title)
                assertEquals(100, migrated.completionRatio)
                assertFalse(migrated.dailyResetEnabled)
                assertNull(migrated.lastDailyResetDate)
                assertFalse(migrated.completionResetSyncPending)
            } finally {
                database.close()
            }
        }
}
