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

import app.cash.turbine.Event
import app.cash.turbine.ReceiveTurbine
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.core.domain.widget.HomeWidgetPlatform
import com.nexters.bandalart.core.domain.widget.HomeWidgetSupport
import com.nexters.bandalart.core.domain.widget.UnsupportedHomeWidgetSupport
import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.home.model.CellType
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomePresenterWidgetGuideTest {
    @Test
    fun firstCompletionFromHomeShowsGuideOnceAndOpensPlatformSteps() =
        runTest {
            val firstTask = cell(id = 12L, title = "첫 번째")
            val secondTask = cell(id = 13L, title = "두 번째")
            val settingsRepository = FakeSettingsRepository(initialTaskCompletionTooltipDismissed = true)

            presenter(repositoryWithTasks(firstTask, secondTask), settingsRepository).test {
                var state = awaitLoadedBandalart()
                state.eventSink(HomeScreen.Event.ToggleTaskCompletion(firstTask))
                state = awaitDialog()

                assertEquals(HomeScreen.DialogState.WidgetGuide(HomeWidgetPlatform.IOS), state.dialog)
                advanceUntilIdle()
                assertEquals(1, settingsRepository.widgetGuideShownMarks)

                state.eventSink(HomeScreen.Event.ShowWidgetGuideSteps)
                do {
                    state = awaitItem()
                } while (state.dialog !is HomeScreen.DialogState.WidgetGuideSteps)
                assertEquals(HomeScreen.DialogState.WidgetGuideSteps(HomeWidgetPlatform.IOS), state.dialog)

                state.eventSink(HomeScreen.Event.DismissDialog)
                do {
                    state = awaitItem()
                } while (state.dialog != null)

                state.eventSink(HomeScreen.Event.ToggleTaskCompletion(secondTask))
                advanceUntilIdle()
                state = expectMostRecentItem()

                assertTrue(state.taskCell(secondTask.id).isCompleted)
                assertNull(state.dialog)
                assertEquals(1, settingsRepository.widgetGuideShownMarks)
            }
        }

    @Test
    fun laterClosesTheGuideAndKeepsItMarkedAsShown() =
        runTest {
            val task = cell(id = 12L, title = "할 일")
            val settingsRepository = FakeSettingsRepository(initialTaskCompletionTooltipDismissed = true)

            presenter(repositoryWithTasks(task), settingsRepository, platform = HomeWidgetPlatform.ANDROID).test {
                var state = awaitLoadedBandalart()
                state.eventSink(HomeScreen.Event.ToggleTaskCompletion(task))
                state = awaitDialog()
                assertEquals(HomeScreen.DialogState.WidgetGuide(HomeWidgetPlatform.ANDROID), state.dialog)

                state.eventSink(HomeScreen.Event.DismissDialog)
                do {
                    state = awaitItem()
                } while (state.dialog != null)
                advanceUntilIdle()

                assertEquals(1, settingsRepository.widgetGuideShownMarks)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun completionSavedFromTaskSheetShowsGuideAfterTheSheetCloses() =
        runTest {
            val task = cell(id = 12L, title = "할 일")
            val settingsRepository = FakeSettingsRepository(initialTaskCompletionTooltipDismissed = true)

            presenter(repositoryWithTasks(task), settingsRepository).test {
                var state = awaitLoadedBandalart()
                state.eventSink(HomeScreen.Event.OpenCell(CellType.TASK, false, task))
                do {
                    state = awaitItem()
                } while (state.bottomSheet !is HomeScreen.BottomSheetState.Cell)
                state.eventSink(HomeScreen.Event.UpdateCompletion(true))
                do {
                    state = awaitItem()
                } while ((state.bottomSheet as? HomeScreen.BottomSheetState.Cell)?.cellData?.isCompleted != true)
                assertNull(state.dialog)

                state.eventSink(HomeScreen.Event.SaveCell)
                state = awaitDialog()

                assertNull(state.bottomSheet)
                assertEquals(HomeScreen.DialogState.WidgetGuide(HomeWidgetPlatform.IOS), state.dialog)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun guideIsNotShownOnUnsupportedDevices() =
        assertNoGuideAfterCompletion(
            settingsRepository = FakeSettingsRepository(initialTaskCompletionTooltipDismissed = true),
            widgetSupport = UnsupportedHomeWidgetSupport,
        )

    @Test
    fun guideIsNotShownAgainAfterItWasShown() =
        assertNoGuideAfterCompletion(
            settingsRepository =
                FakeSettingsRepository(
                    initialTaskCompletionTooltipDismissed = true,
                    initialWidgetGuideShown = true,
                ),
        )

    @Test
    fun uncompletingATaskDoesNotShowTheGuide() =
        assertNoGuideAfterCompletion(
            settingsRepository = FakeSettingsRepository(initialTaskCompletionTooltipDismissed = true),
            task = cell(id = 12L, title = "완료한 일", isCompleted = true),
        )

    @Test
    fun guideWaitsWhileBandalartCompletionIsCelebrated() =
        runTest {
            val task = cell(id = 12L, title = "할 일")
            val settingsRepository = FakeSettingsRepository(initialTaskCompletionTooltipDismissed = true)
            val repository =
                repositoryWithTasks(
                    task,
                    bandalart = bandalart(isCompleted = true),
                    previousBandalartList = listOf(1L to false),
                )

            presenter(repository, settingsRepository).test {
                var state = awaitLoadedBandalart()
                while (state.imageRequest !is HomeScreen.ImageRequest.Complete) state = awaitItem()

                state.eventSink(HomeScreen.Event.ToggleTaskCompletion(task))
                advanceUntilIdle()
                state = expectMostRecentItem()

                assertTrue(state.imageRequest is HomeScreen.ImageRequest.Complete)
                assertNull(state.dialog)
                assertEquals(0, settingsRepository.widgetGuideShownMarks)
            }
        }

    private fun assertNoGuideAfterCompletion(
        settingsRepository: FakeSettingsRepository,
        widgetSupport: HomeWidgetSupport = HomeWidgetSupport { HomeWidgetPlatform.IOS },
        task: BandalartCellEntity = cell(id = 12L, title = "할 일"),
    ) = runTest {
        val repository = repositoryWithTasks(task)
        HomePresenter(
            navigator = FakeNavigator(HomeScreen),
            bandalartRepository = repository,
            bandalartSlotRepository = FakeBandalartSlotRepository(),
            inAppUpdateRepository = FakeInAppUpdateRepository(),
            settingsRepository = settingsRepository,
            homeWidgetSupport = widgetSupport,
        ).test {
            var state = awaitLoadedBandalart()
            state.eventSink(HomeScreen.Event.ToggleTaskCompletion(task))
            do {
                state = awaitItem()
            } while (state.effect !is HomeScreen.Effect.PlayTaskCompletionHaptic)
            advanceUntilIdle()
            state =
                cancelAndConsumeRemainingEvents()
                    .filterIsInstance<Event.Item<HomeScreen.State>>()
                    .lastOrNull()
                    ?.value ?: state

            assertEquals(1, repository.taskCellUpdateCalls)
            assertNull(state.dialog)
            assertEquals(0, settingsRepository.widgetGuideShownMarks)
        }
    }

    private fun repositoryWithTasks(
        vararg taskCells: BandalartCellEntity,
        bandalart: BandalartEntity = bandalart(),
        previousBandalartList: List<Pair<Long, Boolean>> = listOf(bandalart.id to bandalart.isCompleted),
    ): FakeBandalartRepository {
        val mainCell = cell(id = 10L, title = "메인", parentId = null)
        val subCell = cell(id = 11L, title = "서브", parentId = mainCell.id)
        return FakeBandalartRepository(
            initialBandalarts = listOf(bandalart),
            recentBandalartId = bandalart.id,
            previousBandalartList = previousBandalartList,
            mainCells = mapOf(bandalart.id to mainCell),
            childCells =
                mapOf(
                    mainCell.id to listOf(subCell),
                    subCell.id to taskCells.toList(),
                ),
        )
    }

    private fun presenter(
        repository: FakeBandalartRepository,
        settingsRepository: FakeSettingsRepository,
        platform: HomeWidgetPlatform = HomeWidgetPlatform.IOS,
    ) = HomePresenter(
        navigator = FakeNavigator(HomeScreen),
        bandalartRepository = repository,
        bandalartSlotRepository = FakeBandalartSlotRepository(),
        inAppUpdateRepository = FakeInAppUpdateRepository(),
        settingsRepository = settingsRepository,
        homeWidgetSupport = { platform },
    )

    private suspend fun ReceiveTurbine<HomeScreen.State>.awaitLoadedBandalart(): HomeScreen.State {
        var state = awaitItem()
        while (state.bandalartData?.id != 1L) {
            state = awaitItem()
        }
        return state
    }

    private suspend fun ReceiveTurbine<HomeScreen.State>.awaitDialog(): HomeScreen.State {
        var state = awaitItem()
        while (state.dialog == null) {
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

    private fun bandalart(isCompleted: Boolean = false) =
        BandalartEntity(
            id = 1L,
            mainColor = "#3FFFBA",
            subColor = "#111827",
            profileEmoji = "🎯",
            title = "반다라트",
            description = null,
            dueDate = null,
            isCompleted = isCompleted,
            completionRatio = 0,
        )

    private fun cell(
        id: Long,
        title: String?,
        isCompleted: Boolean = false,
        parentId: Long? = 11L,
    ) = BandalartCellEntity(
        id = id,
        title = title,
        description = null,
        dueDate = null,
        isCompleted = isCompleted,
        parentId = parentId,
    )
}
