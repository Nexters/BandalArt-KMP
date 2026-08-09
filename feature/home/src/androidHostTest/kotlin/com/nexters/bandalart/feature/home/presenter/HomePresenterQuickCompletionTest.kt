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

package com.nexters.bandalart.feature.home.presenter

import app.cash.turbine.ReceiveTurbine
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.feature.home.HomeScreen
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomePresenterQuickCompletionTest {
    @Test
    fun validTaskIsCompletedWithPreservedContentAndOneShotEffect() =
        runTest {
            val taskCell =
                cell(
                    id = 12L,
                    title = "매일 걷기",
                    description = "30분",
                    dueDate = "2026-12-31",
                )
            val repository = repositoryWithTasks(taskCell)

            presenter(repository).test {
                var state = awaitLoadedBandalart()
                state.eventSink(HomeScreen.Event.CompleteTask(taskCell))
                do {
                    state = awaitItem()
                } while (state.effect !is HomeScreen.Effect.PlayTaskCompletionHaptic)

                val update = requireNotNull(repository.taskCellUpdate)
                assertEquals(1, repository.taskCellUpdateCalls)
                assertEquals(1L, update.bandalartId)
                assertEquals(taskCell.id, update.cellId)
                assertEquals(taskCell.title, update.entity.title)
                assertEquals(taskCell.description, update.entity.description)
                assertEquals(taskCell.dueDate, update.entity.dueDate)
                assertTrue(update.entity.isCompleted == true)
                assertTrue(state.taskCell(taskCell.id).isCompleted)
                assertEquals(
                    taskCell.id,
                    state.effect.taskCellId,
                )

                state.eventSink(HomeScreen.Event.ConsumeEffect)
                do {
                    state = awaitItem()
                } while (state.effect != null)
                assertNull(state.effect)
            }
        }

    @Test
    fun invalidOrCompletedCellsAreIgnored() =
        runTest {
            val emptyTask = cell(id = 12L, title = null)
            val completedTask = cell(id = 13L, title = "완료", isCompleted = true)
            val repository = repositoryWithTasks(emptyTask, completedTask)

            presenter(repository).test {
                val state = awaitLoadedBandalart()
                advanceUntilIdle()
                expectMostRecentItem()
                val mainCell = requireNotNull(state.bandalartCellData)
                val subCell = mainCell.children.single()

                state.eventSink(HomeScreen.Event.CompleteTask(emptyTask))
                state.eventSink(HomeScreen.Event.CompleteTask(completedTask))
                state.eventSink(HomeScreen.Event.CompleteTask(mainCell))
                state.eventSink(HomeScreen.Event.CompleteTask(subCell))
                yield()

                assertEquals(0, repository.taskCellUpdateCalls)
                assertTrue(state.taskCell(completedTask.id).isCompleted)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun multipleSuccessfulCompletionsEmitHapticsInOrder() =
        runTest {
            val firstTask = cell(id = 12L, title = "첫 번째")
            val secondTask = cell(id = 13L, title = "두 번째")
            val repository = repositoryWithTasks(firstTask, secondTask)

            presenter(repository).test {
                var state = awaitLoadedBandalart()
                state.eventSink(HomeScreen.Event.CompleteTask(firstTask))
                state.eventSink(HomeScreen.Event.CompleteTask(secondTask))

                do {
                    state = awaitItem()
                } while (state.effect !is HomeScreen.Effect.PlayTaskCompletionHaptic)
                assertEquals(firstTask.id, state.effect.taskCellId)

                state.eventSink(HomeScreen.Event.ConsumeEffect)
                do {
                    state = awaitItem()
                } while (
                    state.effect !is HomeScreen.Effect.PlayTaskCompletionHaptic ||
                    state.effect.taskCellId != secondTask.id
                )
                assertEquals(2, repository.taskCellUpdateCalls)

                state.eventSink(HomeScreen.Event.ConsumeEffect)
                do {
                    state = awaitItem()
                } while (state.effect != null)
                assertNull(state.effect)
            }
        }

    @Test
    fun duplicateRequestWhileSavingIsIgnored() =
        runTest {
            val updateStarted = CompletableDeferred<Unit>()
            val allowUpdate = CompletableDeferred<Unit>()
            val taskCell = cell(id = 12L, title = "중복 방지")
            val repository =
                repositoryWithTasks(
                    taskCell,
                    beforeTaskCellUpdate = {
                        updateStarted.complete(Unit)
                        allowUpdate.await()
                    },
                )

            presenter(repository).test {
                var state = awaitLoadedBandalart()
                state.eventSink(HomeScreen.Event.CompleteTask(taskCell))
                updateStarted.await()

                state.eventSink(HomeScreen.Event.CompleteTask(taskCell))
                yield()
                assertEquals(1, repository.taskCellUpdateCalls)

                allowUpdate.complete(Unit)
                do {
                    state = awaitItem()
                } while (state.effect !is HomeScreen.Effect.PlayTaskCompletionHaptic)
                assertEquals(1, repository.taskCellUpdateCalls)
            }
        }

    @Test
    fun failedSaveDoesNotCompleteCellOrEmitHaptic() =
        runTest {
            val updateAttempted = CompletableDeferred<Unit>()
            val taskCell = cell(id = 12L, title = "실패")
            val repository =
                repositoryWithTasks(
                    taskCell,
                    beforeTaskCellUpdate = { updateAttempted.complete(Unit) },
                    taskCellUpdateError = IllegalStateException("save failed"),
                )

            presenter(repository).test {
                val state = awaitLoadedBandalart()
                advanceUntilIdle()
                expectMostRecentItem()
                state.eventSink(HomeScreen.Event.CompleteTask(taskCell))
                updateAttempted.await()
                yield()

                assertEquals(1, repository.taskCellUpdateCalls)
                assertNull(repository.taskCellUpdate)
                assertFalse(state.taskCell(taskCell.id).isCompleted)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun repositoryWithTasks(
        vararg taskCells: BandalartCellEntity,
        beforeTaskCellUpdate: suspend () -> Unit = {},
        taskCellUpdateError: Throwable? = null,
    ): FakeBandalartRepository {
        val mainCell = cell(id = 10L, title = "메인")
        val subCell = cell(id = 11L, title = "서브", parentId = mainCell.id)
        return FakeBandalartRepository(
            initialBandalarts = listOf(bandalart()),
            recentBandalartId = 1L,
            mainCells = mapOf(1L to mainCell),
            childCells =
                mapOf(
                    mainCell.id to listOf(subCell),
                    subCell.id to taskCells.toList(),
                ),
            beforeTaskCellUpdate = beforeTaskCellUpdate,
            taskCellUpdateError = taskCellUpdateError,
        )
    }

    private fun presenter(repository: FakeBandalartRepository) =
        HomePresenter(
            navigator = FakeNavigator(HomeScreen),
            bandalartRepository = repository,
            bandalartSlotRepository = FakeBandalartSlotRepository(),
            inAppUpdateRepository = FakeInAppUpdateRepository(),
            settingsRepository = FakeSettingsRepository(),
        )

    private suspend fun ReceiveTurbine<HomeScreen.State>.awaitLoadedBandalart(): HomeScreen.State {
        var state = awaitItem()
        while (state.bandalartData?.id != 1L || state.isLoading) {
            state = awaitItem()
        }
        return state
    }

    private fun HomeScreen.State.taskCell(cellId: Long): BandalartCellEntity =
        requireNotNull(
            bandalartCellData
                ?.children
                ?.flatMap { subCell -> subCell.children }
                ?.firstOrNull { taskCell -> taskCell.id == cellId },
        )

    private fun bandalart() =
        BandalartEntity(
            id = 1L,
            mainColor = "#3FFFBA",
            subColor = "#111827",
            profileEmoji = "🎯",
            title = "반다라트",
            description = null,
            dueDate = null,
            isCompleted = false,
            completionRatio = 0,
        )

    private fun cell(
        id: Long,
        title: String?,
        description: String? = null,
        dueDate: String? = null,
        isCompleted: Boolean = false,
        parentId: Long? = 11L,
    ) = BandalartCellEntity(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate,
        isCompleted = isCompleted,
        parentId = parentId,
    )
}
