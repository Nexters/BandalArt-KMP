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
import com.nexters.bandalart.core.database.entity.BandalartCellDBEntity
import com.nexters.bandalart.core.database.entity.BandalartDBEntity
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

@ExtendWith(RobolectricExtension::class)
@Config(sdk = [35])
class BandalartBackupDaoTest {
    private lateinit var database: BandalartDatabase
    private lateinit var dao: BandalartDao

    @BeforeEach
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BandalartDatabase::class.java).build()
        dao = database.bandalartDao
    }

    @AfterEach
    fun tearDown() {
        database.close()
    }

    @Test
    fun backupRestoreReplacesAllRowsWhilePreservingIds() =
        runTest {
            dao.createEmptyBandalart()
            val restoredBandalart = BandalartDBEntity(id = 42L, title = "복원된 목표")
            val restoredCell = BandalartCellDBEntity(id = 84L, bandalartId = 42L, title = "복원된 목표")

            dao.replaceAllForBackup(listOf(restoredBandalart), listOf(restoredCell))

            assertEquals(listOf(restoredBandalart), dao.getAllBandalarts())
            assertEquals(listOf(restoredCell), dao.getAllCells())
        }

    @Test
    fun backupRestoreFailureRollsBackThePreviousRows() =
        runTest {
            val previousId = dao.createEmptyBandalart()
            val previousBandalarts = dao.getAllBandalarts()
            val previousCells = dao.getAllCells()
            val invalidCell = BandalartCellDBEntity(id = 84L, bandalartId = 999L)

            val failure =
                runCatching {
                    dao.replaceAllForBackup(
                        bandalarts = listOf(BandalartDBEntity(id = 42L)),
                        cells = listOf(invalidCell),
                    )
                }.exceptionOrNull()

            assertTrue(failure != null)
            assertEquals(previousId, previousBandalarts.single().id)
            assertEquals(previousBandalarts, dao.getAllBandalarts())
            assertEquals(previousCells, dao.getAllCells())
        }
}
