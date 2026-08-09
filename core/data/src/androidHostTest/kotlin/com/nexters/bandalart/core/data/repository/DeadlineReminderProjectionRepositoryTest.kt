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

package com.nexters.bandalart.core.data.repository

import com.nexters.bandalart.core.database.BandalartDao
import com.nexters.bandalart.core.database.entity.BandalartCellDBEntity
import com.nexters.bandalart.core.domain.notification.DeadlineReminderCandidate
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeadlineReminderProjectionRepositoryTest {
    private val bandalartDao = mockk<BandalartDao>()
    private val repository = DefaultDeadlineReminderProjectionRepository(bandalartDao)

    @Test
    fun allPersistedCellsAreProjectedWithoutChangingStoredDueDates() =
        runTest {
            coEvery { bandalartDao.getAllCells() } returns
                listOf(
                    cell(id = 1, bandalartId = 10, title = "main", dueDate = "2026-08-10T00:00"),
                    cell(id = 2, bandalartId = 10, title = "sub", dueDate = "2026-08-11T12:30:45"),
                    cell(id = 3, bandalartId = 10, title = "task", dueDate = null, isCompleted = true),
                )

            assertEquals(
                listOf(
                    DeadlineReminderCandidate(1, 10, "main", "2026-08-10T00:00", false),
                    DeadlineReminderCandidate(2, 10, "sub", "2026-08-11T12:30:45", false),
                    DeadlineReminderCandidate(3, 10, "task", null, true),
                ),
                repository.getCandidates(),
            )
            coVerify(exactly = 1) { bandalartDao.getAllCells() }
        }

    private fun cell(
        id: Long,
        bandalartId: Long,
        title: String?,
        dueDate: String?,
        isCompleted: Boolean = false,
    ) = BandalartCellDBEntity(
        id = id,
        bandalartId = bandalartId,
        title = title,
        dueDate = dueDate,
        isCompleted = isCompleted,
    )
}
