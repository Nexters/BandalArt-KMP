/*
 * Copyright 2026 easyhooon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nexters.bandalart.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nexters.bandalart.core.database.BandalartDao
import com.nexters.bandalart.core.database.BandalartDatabase
import com.nexters.bandalart.core.database.entity.CreateBandalartDto
import com.nexters.bandalart.core.database.entity.CreateBandalartSubGoalDto
import com.nexters.bandalart.core.datastore.BandalartDataStore
import com.nexters.bandalart.core.domain.entity.UpdateBandalartTaskCellEntity
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import io.mockk.mockk
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
class BandalartWidgetRepositoryTest {
    private lateinit var database: BandalartDatabase
    private lateinit var dao: BandalartDao
    private lateinit var bandalartRepository: BandalartRepository
    private lateinit var repository: DefaultBandalartWidgetRepository

    @BeforeEach
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BandalartDatabase::class.java).build()
        dao = database.bandalartDao
        bandalartRepository = DefaultBandalartRepository(mockk<BandalartDataStore>(), dao)
        repository =
            DefaultBandalartWidgetRepository(
                bandalartRepository = bandalartRepository,
                bandalartDao = dao,
            )
    }

    @AfterEach
    fun tearDown() {
        database.close()
    }

    @Test
    fun snapshotContainsOnlyTitledTasksFromTheSelectedSubGoal() =
        runTest {
            val bandalartId =
                dao.createBandalartTree(
                    CreateBandalartDto(
                        title = "건강한 생활",
                        profileEmoji = "💪",
                        subGoals =
                            listOf(
                                CreateBandalartSubGoalDto(
                                    title = "운동",
                                    tasks = listOf("달리기", "  ", "스트레칭"),
                                ),
                            ),
                    ),
                )
            val subGoalId =
                dao
                    .getBandalartMainCell(bandalartId)
                    .children
                    .first()
                    .id!!

            val snapshot = repository.getSnapshot(bandalartId, subGoalId)

            assertEquals("건강한 생활", snapshot?.title)
            assertEquals("💪", snapshot?.profileEmoji)
            assertEquals("운동", snapshot?.subGoalTitle)
            assertEquals(listOf("달리기", "스트레칭"), snapshot?.tasks?.map { it.title })
        }

    @Test
    fun completingATaskPreservesItsContentAndReturnsTheRecalculatedSnapshot() =
        runTest {
            val bandalartId = createBandalart()
            val subGoalId =
                dao
                    .getBandalartMainCell(bandalartId)
                    .children
                    .first()
                    .id!!
            val taskId = dao.getChildCells(subGoalId).first().id!!
            bandalartRepository.updateBandalartTaskCell(
                bandalartId = bandalartId,
                cellId = taskId,
                updateBandalartTaskCellEntity =
                    UpdateBandalartTaskCellEntity(
                        title = "달리기",
                        description = "한강 5km",
                        dueDate = "2026-08-11T07:00",
                        isCompleted = false,
                    ),
            )

            val snapshot =
                repository.setTaskCompleted(
                    bandalartId = bandalartId,
                    subGoalId = subGoalId,
                    taskId = taskId,
                    completed = true,
                )
            val storedTask = bandalartRepository.getChildCells(subGoalId).first { it.id == taskId }

            assertEquals(true, snapshot?.tasks?.first { it.id == taskId }?.isCompleted)
            assertEquals(4, snapshot?.completionRatio)
            assertEquals("달리기", storedTask.title)
            assertEquals("한강 5km", storedTask.description)
            assertEquals("2026-08-11T07:00", storedTask.dueDate)
        }

    @Test
    fun taskFromAnotherBandalartIsNotChanged() =
        runTest {
            val selectedBandalartId = createBandalart()
            val selectedSubGoalId =
                dao
                    .getBandalartMainCell(selectedBandalartId)
                    .children
                    .first()
                    .id!!
            val otherBandalartId = createBandalart()
            val otherSubGoalId =
                dao
                    .getBandalartMainCell(otherBandalartId)
                    .children
                    .first()
                    .id!!
            val otherTaskId = dao.getChildCells(otherSubGoalId).first().id!!

            val snapshot =
                repository.setTaskCompleted(
                    bandalartId = selectedBandalartId,
                    subGoalId = selectedSubGoalId,
                    taskId = otherTaskId,
                    completed = true,
                )

            assertFalse(bandalartRepository.getChildCells(otherSubGoalId).first().isCompleted)
            assertEquals(selectedBandalartId, snapshot?.bandalartId)
            assertEquals(0, snapshot?.completionRatio)
        }

    @Test
    fun taskFromAnotherSubGoalIsNotChanged() =
        runTest {
            val bandalartId = createBandalart()
            val subGoals = dao.getBandalartMainCell(bandalartId).children
            val selectedSubGoalId = subGoals.first().id!!
            val otherSubGoalId = subGoals[1].id!!
            val otherTaskId = dao.getChildCells(otherSubGoalId).first().id!!
            bandalartRepository.updateBandalartTaskCell(
                bandalartId = bandalartId,
                cellId = otherTaskId,
                updateBandalartTaskCellEntity =
                    UpdateBandalartTaskCellEntity(
                        title = "수영",
                        description = null,
                        dueDate = null,
                        isCompleted = false,
                    ),
            )

            repository.setTaskCompleted(
                bandalartId = bandalartId,
                subGoalId = selectedSubGoalId,
                taskId = otherTaskId,
                completed = true,
            )

            assertFalse(bandalartRepository.getChildCells(otherSubGoalId).first().isCompleted)
        }

    @Test
    fun blankTaskCannotBeCompleted() =
        runTest {
            val bandalartId = createBandalart()
            val subGoalId =
                dao
                    .getBandalartMainCell(bandalartId)
                    .children
                    .first()
                    .id!!
            val blankTask = dao.getChildCells(subGoalId)[1]

            val snapshot =
                repository.setTaskCompleted(
                    bandalartId = bandalartId,
                    subGoalId = subGoalId,
                    taskId = blankTask.id!!,
                    completed = true,
                )

            assertFalse(bandalartRepository.getChildCells(subGoalId)[1].isCompleted)
            assertEquals(listOf("달리기"), snapshot?.tasks?.map { it.title })
            assertEquals(0, snapshot?.completionRatio)
        }

    @Test
    fun settingTheSameCompletionValueTwiceIsIdempotent() =
        runTest {
            val bandalartId = createBandalart()
            val subGoalId =
                dao
                    .getBandalartMainCell(bandalartId)
                    .children
                    .first()
                    .id!!
            val taskId = dao.getChildCells(subGoalId).first().id!!

            repository.setTaskCompleted(bandalartId, subGoalId, taskId, completed = true)
            val secondSnapshot = repository.setTaskCompleted(bandalartId, subGoalId, taskId, completed = true)

            assertEquals(true, secondSnapshot?.tasks?.first()?.isCompleted)
            assertEquals(4, secondSnapshot?.completionRatio)
        }

    @Test
    fun deletedSelectionReturnsNull() =
        runTest {
            val bandalartId = createBandalart()
            val subGoalId =
                dao
                    .getBandalartMainCell(bandalartId)
                    .children
                    .first()
                    .id!!
            bandalartRepository.deleteBandalart(bandalartId)

            assertNull(repository.getSnapshot(bandalartId, subGoalId))
            assertNull(repository.setTaskCompleted(bandalartId, subGoalId, taskId = 1L, completed = true))
        }

    @Test
    fun resetSubGoalSelectionReturnsNull() =
        runTest {
            val bandalartId = createBandalart()
            val subGoalId = dao.getBandalartMainCell(bandalartId).children.first().id!!
            bandalartRepository.deleteBandalartCell(subGoalId)

            assertNull(repository.getSnapshot(bandalartId, subGoalId))
        }

    @Test
    fun taskUnderResetSubGoalCannotBeCompleted() =
        runTest {
            val bandalartId = createBandalart()
            val subGoalId = dao.getBandalartMainCell(bandalartId).children.first().id!!
            val taskId = dao.getChildCells(subGoalId).first().id!!
            bandalartRepository.deleteBandalartCell(subGoalId)
            bandalartRepository.updateBandalartTaskCell(
                bandalartId = bandalartId,
                cellId = taskId,
                updateBandalartTaskCellEntity =
                    UpdateBandalartTaskCellEntity(
                        title = "다시 입력된 태스크",
                        description = null,
                        dueDate = null,
                        isCompleted = false,
                    ),
            )

            val snapshot = repository.setTaskCompleted(bandalartId, subGoalId, taskId, completed = true)
            val storedTask = bandalartRepository.getChildCells(subGoalId).first { it.id == taskId }

            assertFalse(storedTask.isCompleted)
            assertNull(snapshot)
        }

    @Test
    fun snapshotWithoutASubGoalContainsOnlyBandalartSummary() =
        runTest {
            val bandalartId = createBandalart()

            val snapshot = repository.getSnapshot(bandalartId, subGoalId = null)

            assertEquals(bandalartId, snapshot?.bandalartId)
            assertNull(snapshot?.subGoalId)
            assertNull(snapshot?.subGoalTitle)
            assertEquals(emptyList<String>(), snapshot?.tasks?.map { it.title })
        }

    private suspend fun createBandalart(): Long =
        dao.createBandalartTree(
            CreateBandalartDto(
                title = "건강한 생활",
                profileEmoji = "💪",
                subGoals =
                    listOf(
                        CreateBandalartSubGoalDto(
                            title = "운동",
                            tasks = listOf("달리기"),
                        ),
                    ),
            ),
        )
}
