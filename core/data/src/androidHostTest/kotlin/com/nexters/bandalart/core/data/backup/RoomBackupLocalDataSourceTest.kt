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

package com.nexters.bandalart.core.data.backup

import com.nexters.bandalart.core.database.BandalartDao
import com.nexters.bandalart.core.database.entity.BandalartCellDBEntity
import com.nexters.bandalart.core.database.entity.BandalartDBEntity
import com.nexters.bandalart.core.datastore.BandalartBackupPreferences
import com.nexters.bandalart.core.datastore.BandalartDataStore
import com.nexters.bandalart.core.domain.backup.BackupBandalart
import com.nexters.bandalart.core.domain.backup.BackupCell
import com.nexters.bandalart.core.domain.backup.BackupCompletedBandalart
import com.nexters.bandalart.core.domain.backup.BackupPreferences
import com.nexters.bandalart.core.domain.backup.BackupSnapshot
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoomBackupLocalDataSourceTest {
    private val dao = mockk<BandalartDao>()
    private val dataStore = mockk<BandalartDataStore>()

    @Test
    fun createSnapshotMapsAllRoomRowsAndDurablePreferences() =
        runTest {
            val bandalart = BandalartDBEntity(id = 1L, title = "목표")
            val cell = BandalartCellDBEntity(id = 10L, bandalartId = 1L, title = "목표")
            coEvery { dao.getAllBandalarts() } returns listOf(bandalart)
            coEvery { dao.getAllCells() } returns listOf(cell)
            coEvery { dataStore.createBackupPreferences(listOf(1L)) } returns storedPreferences()
            val source = RoomBackupLocalDataSource(dao, dataStore)

            val snapshot = source.createSnapshot()

            assertEquals(listOf(BackupBandalart(id = 1L, title = "목표", mainColor = "#FF3FFFBA", subColor = "#FF111827")), snapshot.bandalarts)
            assertEquals(listOf(BackupCell(id = 10L, bandalartId = 1L, title = "목표")), snapshot.cells)
            assertEquals(1L, snapshot.preferences.recentBandalartId)
            assertEquals(listOf(BackupCompletedBandalart(1L, true)), snapshot.preferences.completedBandalarts)
        }

    @Test
    fun restoreSnapshotReplacesRoomBeforeRestoringPreferences() =
        runTest {
            val snapshot = validSnapshot()
            coEvery { dao.getAllBandalarts() } returns listOf(BandalartDBEntity(id = 9L))
            coEvery { dao.replaceAllForBackup(any(), any()) } returns Unit
            coEvery { dataStore.restoreBackupPreferences(any(), any()) } returns Unit
            val source = RoomBackupLocalDataSource(dao, dataStore)

            source.restoreSnapshot(snapshot)

            coVerifyOrder {
                dao.getAllBandalarts()
                dao.replaceAllForBackup(
                    bandalarts = listOf(BandalartDBEntity(id = 1L, title = "목표")),
                    cells = listOf(BandalartCellDBEntity(id = 10L, bandalartId = 1L, title = "목표")),
                )
                dataStore.restoreBackupPreferences(
                    backup = storedPreferences(),
                    bandalartIdsToClear = listOf(9L, 1L),
                )
            }
        }

    @Test
    fun hasDataUsesPersistedBandalarts() =
        runTest {
            coEvery { dao.getAllBandalarts() } returns listOf(BandalartDBEntity(id = 1L))

            assertTrue(RoomBackupLocalDataSource(dao, dataStore).hasData())
        }

    private fun validSnapshot() =
        BackupSnapshot(
            bandalarts = listOf(BackupBandalart(id = 1L, title = "목표", mainColor = "#FF3FFFBA", subColor = "#FF111827")),
            cells = listOf(BackupCell(id = 10L, bandalartId = 1L, title = "목표")),
            preferences =
                BackupPreferences(
                    recentBandalartId = 1L,
                    recentSubGoalIds = emptyMap(),
                    completedBandalarts = listOf(BackupCompletedBandalart(1L, true)),
                    onboardingCompleted = true,
                    themeMode = "dark",
                    recentEmojis = listOf("🎯"),
                    deadlineReminderEnabled = true,
                    maxBandalartSlots = 1,
                ),
        )

    private fun storedPreferences() =
        BandalartBackupPreferences(
            recentBandalartId = 1L,
            recentSubGoalIds = emptyMap(),
            completedBandalarts = listOf(1L to true),
            onboardingCompleted = true,
            themeMode = "dark",
            recentEmojis = listOf("🎯"),
            deadlineReminderEnabled = true,
            maxBandalartSlots = 1,
        )
}
