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
                state.eventSink(HomeScreen.Event.ToggleTaskCompletion(taskCell))
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
    fun completedTaskIsUncompletedWithPreservedContentAndOneShotEffect() =
        runTest {
            val taskCell =
                cell(
                    id = 12L,
                    title = "매일 걷기",
                    description = "30분",
                    dueDate = "2026-12-31",
                    isCompleted = true,
                )
            val repository = repositoryWithTasks(taskCell)

            presenter(repository).test {
                var state = awaitLoadedBandalart()
                state.eventSink(HomeScreen.Event.ToggleTaskCompletion(taskCell))
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
                assertFalse(update.entity.isCompleted == true)
                assertFalse(state.taskCell(taskCell.id).isCompleted)
                assertEquals(taskCell.id, state.effect.taskCellId)
            }
        }

    @Test
    fun invalidCellsAreIgnored() =
        runTest {
            val emptyTask = cell(id = 12L, title = null)
            val repository = repositoryWithTasks(emptyTask)

            presenter(repository).test {
                val state = awaitLoadedBandalart()
                advanceUntilIdle()
                expectMostRecentItem()
                val mainCell = requireNotNull(state.bandalartCellData)
                val subCell = mainCell.children.single()

                state.eventSink(HomeScreen.Event.ToggleTaskCompletion(emptyTask))
                state.eventSink(HomeScreen.Event.ToggleTaskCompletion(mainCell))
                state.eventSink(HomeScreen.Event.ToggleTaskCompletion(subCell))
                yield()

                assertEquals(0, repository.taskCellUpdateCalls)
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
                state.eventSink(HomeScreen.Event.ToggleTaskCompletion(firstTask))
                state.eventSink(HomeScreen.Event.ToggleTaskCompletion(secondTask))

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
                state.eventSink(HomeScreen.Event.ToggleTaskCompletion(taskCell))
                updateStarted.await()

                state.eventSink(HomeScreen.Event.ToggleTaskCompletion(taskCell))
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
    fun selectionDuringToggleDoesNotReplaceNewBandalartCells() =
        runTest {
            val updateStarted = CompletableDeferred<Unit>()
            val allowUpdate = CompletableDeferred<Unit>()
            val firstMain = cell(id = 10L, title = "첫 메인", parentId = null)
            val firstSub = cell(id = 11L, title = "첫 서브", parentId = firstMain.id)
            val firstTask = cell(id = 12L, title = "첫 목표", parentId = firstSub.id)
            val secondMain = cell(id = 20L, title = "둘째 메인", parentId = null)
            val secondSub = cell(id = 21L, title = "둘째 서브", parentId = secondMain.id)
            val secondTask = cell(id = 22L, title = "둘째 목표", parentId = secondSub.id)
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1L), bandalart(2L)),
                    recentBandalartId = 1L,
                    mainCells = mapOf(1L to firstMain, 2L to secondMain),
                    childCells =
                        mapOf(
                            firstMain.id to listOf(firstSub),
                            firstSub.id to listOf(firstTask),
                            secondMain.id to listOf(secondSub),
                            secondSub.id to listOf(secondTask),
                        ),
                    beforeTaskCellUpdate = {
                        updateStarted.complete(Unit)
                        allowUpdate.await()
                    },
                )

            presenter(repository).test {
                var state = awaitLoadedBandalart(1L)
                state.eventSink(HomeScreen.Event.ToggleTaskCompletion(firstTask))
                updateStarted.await()

                state.eventSink(HomeScreen.Event.SelectBandalart(2L))
                state = awaitLoadedBandalart(2L)
                assertEquals("둘째 목표", state.taskCell(secondTask.id).title)

                allowUpdate.complete(Unit)
                advanceUntilIdle()
                state = expectMostRecentItem()

                assertEquals(1, repository.taskCellUpdateCalls)
                assertEquals(2L, state.bandalartData?.id)
                assertEquals("둘째 목표", state.taskCell(secondTask.id).title)
                assertEquals(2L, repository.recentBandalartId)
                assertNull(state.effect)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun roomRefreshDuringTogglePreservesLatestParentCompletion() =
        runTest {
            val updateStarted = CompletableDeferred<Unit>()
            val allowUpdate = CompletableDeferred<Unit>()
            val taskCell = cell(id = 12L, title = "목표")
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
                state.eventSink(HomeScreen.Event.ToggleTaskCompletion(taskCell))
                updateStarted.await()

                repository.publishBandalartRevision(
                    bandalart = bandalart().copy(completionRatio = 1),
                    mainCell = requireNotNull(state.bandalartCellData).copy(isCompleted = true),
                )
                do {
                    state = awaitItem()
                } while (state.bandalartCellData?.isCompleted != true || state.isLoading)

                allowUpdate.complete(Unit)
                do {
                    state = awaitItem()
                } while (state.effect !is HomeScreen.Effect.PlayTaskCompletionHaptic)

                assertTrue(state.bandalartCellData?.isCompleted == true)
                assertTrue(state.taskCell(taskCell.id).isCompleted)
            }
        }

    @Test
    fun taskResetDuringTogglePreservesLatestRoomStateWithoutHaptic() =
        runTest {
            val updatePersisted = CompletableDeferred<Unit>()
            val allowUpdateReturn = CompletableDeferred<Unit>()
            val taskCell = cell(id = 12L, title = "삭제될 목표")
            val repository =
                repositoryWithTasks(
                    taskCell,
                    afterTaskCellUpdate = {
                        updatePersisted.complete(Unit)
                        allowUpdateReturn.await()
                    },
                )

            presenter(repository).test {
                var state = awaitLoadedBandalart()
                state.eventSink(HomeScreen.Event.ToggleTaskCompletion(taskCell))
                updatePersisted.await()

                repository.publishTaskCellRevision(taskCell.copy(title = null, isCompleted = false))
                repository.publishBandalartRevision(bandalart().copy(completionRatio = 2))
                do {
                    state = awaitItem()
                } while (state.taskCell(taskCell.id).title != null || state.isLoading)

                allowUpdateReturn.complete(Unit)
                advanceUntilIdle()

                assertNull(state.taskCell(taskCell.id).title)
                assertFalse(state.taskCell(taskCell.id).isCompleted)
                assertNull(state.effect)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun authoritativeRefreshFailureDoesNotCrashOrEmitHaptic() =
        runTest {
            var failBandalartLoad = false
            val taskCell = cell(id = 12L, title = "저장은 성공")
            val repository =
                repositoryWithTasks(
                    taskCell,
                    afterTaskCellUpdate = { failBandalartLoad = true },
                    beforeBandalartLoad = {
                        if (failBandalartLoad) error("refresh failed")
                    },
                )

            presenter(repository).test {
                val state = awaitLoadedBandalart()
                advanceUntilIdle()
                expectMostRecentItem()
                state.eventSink(HomeScreen.Event.ToggleTaskCompletion(taskCell))
                advanceUntilIdle()

                assertEquals(1, repository.taskCellUpdateCalls)
                assertFalse(state.taskCell(taskCell.id).isCompleted)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
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
                state.eventSink(HomeScreen.Event.ToggleTaskCompletion(taskCell))
                updateAttempted.await()
                yield()

                assertEquals(1, repository.taskCellUpdateCalls)
                assertNull(repository.taskCellUpdate)
                assertFalse(state.taskCell(taskCell.id).isCompleted)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun failedSaveDoesNotUncompleteCellOrEmitHaptic() =
        runTest {
            val updateAttempted = CompletableDeferred<Unit>()
            val taskCell = cell(id = 12L, title = "실패", isCompleted = true)
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
                state.eventSink(HomeScreen.Event.ToggleTaskCompletion(taskCell))
                updateAttempted.await()
                yield()

                assertEquals(1, repository.taskCellUpdateCalls)
                assertNull(repository.taskCellUpdate)
                assertTrue(state.taskCell(taskCell.id).isCompleted)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun repositoryWithTasks(
        vararg taskCells: BandalartCellEntity,
        beforeTaskCellUpdate: suspend () -> Unit = {},
        afterTaskCellUpdate: suspend () -> Unit = {},
        beforeBandalartLoad: suspend (Long) -> Unit = {},
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
            afterTaskCellUpdate = afterTaskCellUpdate,
            beforeBandalartLoad = beforeBandalartLoad,
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

    private suspend fun ReceiveTurbine<HomeScreen.State>.awaitLoadedBandalart(
        bandalartId: Long = 1L,
    ): HomeScreen.State {
        var state = awaitItem()
        while (state.bandalartData?.id != bandalartId || state.isLoading) {
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

    private fun bandalart(id: Long = 1L) =
        BandalartEntity(
            id = id,
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
