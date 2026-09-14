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

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val BANDALART_MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "ALTER TABLE `bandalarts` ADD COLUMN `dailyResetEnabled` INTEGER NOT NULL DEFAULT 0",
            )
            connection.execSQL(
                "ALTER TABLE `bandalarts` ADD COLUMN `lastDailyResetDate` TEXT DEFAULT NULL",
            )
            connection.execSQL(
                "ALTER TABLE `bandalarts` ADD COLUMN `completionResetSyncPending` INTEGER NOT NULL DEFAULT 0",
            )
        }
    }

fun RoomDatabase.Builder<BandalartDatabase>.addBandalartMigrations(): RoomDatabase.Builder<BandalartDatabase> = addMigrations(BANDALART_MIGRATION_1_2)
