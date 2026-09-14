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

import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.core.domain.widget.BandalartWidgetLaunchTarget
import com.nexters.bandalart.core.domain.widget.BufferedBandalartWidgetLaunchTarget
import com.nexters.bandalart.core.navigation.CloudBackupScreen
import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.home.model.CellType
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HomePresenterTest {
    @Test
    fun cloudBackupSettingsOpensDedicatedScreen() =
        runTest {
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1L)),
                    recentBandalartId = 1L,
                )
            val navigator = FakeNavigator(HomeScreen)
            val presenter =
                HomePresenter(
                    navigator = navigator,
                    bandalartRepository = repository,
                    bandalartSlotRepository = FakeBandalartSlotRepository(),
                    inAppUpdateRepository = FakeInAppUpdateRepository(),
                    settingsRepository = FakeSettingsRepository(),
                )

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData?.id != 1L) state = awaitItem()

                state.eventSink(HomeScreen.Event.OpenCloudBackup)

                assertEquals(
                    CloudBackupScreen(entryPoint = CloudBackupScreen.EntryPoint.SETTINGS),
                    navigator.awaitNextScreen(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun openingASubgoalAndItsTaskRecordsTheSubgoalForTheCurrentBandalart() =
        runTest {
            val mainCell = cell(id = 20L, parentId = null)
            val subCell = cell(id = 21L, parentId = mainCell.id)
            val taskCell = cell(id = 22L, parentId = subCell.id)
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(2L)),
                    recentBandalartId = 2L,
                    mainCells = mapOf(2L to mainCell),
                    childCells = mapOf(mainCell.id to listOf(subCell), subCell.id to listOf(taskCell)),
                )

            presenter(repository).test {
                var state = awaitItem()
                while (state.bandalartData?.id != 2L) {
                    state = awaitItem()
                }

                state.eventSink(HomeScreen.Event.OpenCell(CellType.SUB, false, subCell))
                advanceUntilIdle()
                assertEquals(21L, repository.recentSubGoalIds[2L])

                state.eventSink(HomeScreen.Event.OpenCell(CellType.TASK, false, taskCell))
                advanceUntilIdle()
                assertEquals(21L, repository.recentSubGoalIds[2L])
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun widgetLaunchSelectsAndAcknowledgesAValidTarget() =
        runTest {
            val target = BufferedBandalartWidgetLaunchTarget().apply { record(2L) }
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1L), bandalart(2L)),
                    recentBandalartId = 1L,
                )

            presenter(repository, target).test {
                var state = awaitItem()
                while (state.bandalartData?.id != 2L || target.pendingBandalartId.value != null) {
                    state = awaitItem()
                }

                assertEquals(2L, repository.recentBandalartId)
                assertNull(target.pendingBandalartId.value)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun widgetLaunchAcknowledgesAMissingTargetWithoutChangingTheFallbackSelection() =
        runTest {
            val target = BufferedBandalartWidgetLaunchTarget().apply { record(99L) }
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1L), bandalart(2L)),
                    recentBandalartId = 1L,
                )

            presenter(repository, target).test {
                var state = awaitItem()
                while (state.bandalartData?.id != 1L || target.pendingBandalartId.value != null) {
                    state = awaitItem()
                }

                assertEquals(1L, repository.recentBandalartId)
                assertNull(target.pendingBandalartId.value)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun mostRecentlyOpenedBandalartAndCellTreeAreLoaded() =
        runTest {
            val mainCell = cell(id = 20L, parentId = null)
            val subCell = cell(id = 21L, parentId = mainCell.id)
            val taskCell = cell(id = 22L, parentId = subCell.id)
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1L), bandalart(2L)),
                    recentBandalartId = 2L,
                    mainCells = mapOf(2L to mainCell),
                    childCells =
                        mapOf(
                            mainCell.id to listOf(subCell),
                            subCell.id to listOf(taskCell),
                        ),
                )
            val presenter = presenter(repository)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData?.id != 2L) {
                    state = awaitItem()
                }

                assertEquals(2, state.bandalartList.size)
                assertEquals(2L, state.bandalartData.id)
                assertEquals(mainCell.id, state.bandalartCellData?.id)
                assertEquals(
                    subCell.id,
                    state.bandalartCellData
                        ?.children
                        ?.single()
                        ?.id,
                )
                assertEquals(
                    taskCell.id,
                    state.bandalartCellData
                        ?.children
                        ?.single()
                        ?.children
                        ?.single()
                        ?.id,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun firstBandalartIsUsedWhenRecentIdIsMissing() =
        runTest {
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1L), bandalart(2L)),
                    recentBandalartId = 99L,
                )
            val presenter = presenter(repository)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData == null) {
                    state = awaitItem()
                }

                assertEquals(1L, state.bandalartData.id)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun emptyListCreatesOneInitialBandalart() =
        runTest {
            val created = bandalart(3L)
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = emptyList(),
                    createdBandalart = created,
                )
            val presenter = presenter(repository)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData?.id != created.id) {
                    state = awaitItem()
                }

                assertEquals(1, repository.createCalls)
                assertEquals(created.id, repository.recentBandalartId)
                assertTrue(repository.completionUpdates.contains(created.id to false))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun newlyCompletedBandalartIsSelectedWithoutAdvancingCompletionSnapshot() =
        runTest {
            val repository =
                FakeBandalartRepository(
                    initialBandalarts =
                        listOf(
                            bandalart(1L),
                            bandalart(2L, isCompleted = true),
                        ),
                    recentBandalartId = 1L,
                    previousBandalartList = listOf(1L to false, 2L to false),
                )
            val presenter = presenter(repository)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData?.id != 2L) {
                    state = awaitItem()
                }

                assertTrue(state.isBandalartCompleted)
                assertTrue(repository.completionUpdates.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun selectingBandalartUpdatesRecentIdAndDetail() =
        runTest {
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1L), bandalart(2L)),
                    recentBandalartId = 1L,
                )
            val presenter = presenter(repository)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData?.id != 1L) {
                    state = awaitItem()
                }

                state.eventSink(HomeScreen.Event.SelectBandalart(2L))
                do {
                    state = awaitItem()
                } while (state.bandalartData?.id != 2L)

                assertEquals(2L, repository.recentBandalartId)
                assertFalse(state.isBandalartCompleted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun singleBandalartTopBarActionOpensCreationOptionsWithoutCreating() =
        runTest {
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1L)),
                    recentBandalartId = 1L,
                )

            presenter(repository).test {
                var state = awaitItem()
                while (state.bandalartData?.id != 1L) {
                    state = awaitItem()
                }

                state.eventSink(HomeScreen.Event.OpenBandalartList)
                do {
                    state = awaitItem()
                } while (state.bottomSheet !is HomeScreen.BottomSheetState.BandalartList)

                val sheet = state.bottomSheet
                assertTrue(sheet.isCreationOptionsVisible)
                assertEquals(0, repository.createCalls)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun addButtonSwitchesExistingListSheetToCreationOptionsAndBack() =
        runTest {
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1L), bandalart(2L)),
                    recentBandalartId = 1L,
                )

            presenter(repository).test {
                var state = awaitItem()
                while (state.bandalartData?.id != 1L) {
                    state = awaitItem()
                }
                state.eventSink(HomeScreen.Event.OpenBandalartList)
                do {
                    state = awaitItem()
                } while (state.bottomSheet !is HomeScreen.BottomSheetState.BandalartList)

                state.eventSink(HomeScreen.Event.OpenBandalartCreationOptions)
                do {
                    state = awaitItem()
                } while (
                    (state.bottomSheet as? HomeScreen.BottomSheetState.BandalartList)
                        ?.isCreationOptionsVisible != true
                )

                state.eventSink(HomeScreen.Event.CloseBandalartCreationOptions)
                do {
                    state = awaitItem()
                } while (
                    (state.bottomSheet as? HomeScreen.BottomSheetState.BandalartList)
                        ?.isCreationOptionsVisible != false
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun taskCompletionTooltipIsShownOnlyUntilPermanentlyDismissed() =
        runTest {
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1L)),
                    recentBandalartId = 1L,
                )
            val settingsRepository =
                FakeSettingsRepository(initialRoutineSettingsTooltipDismissed = true)

            presenter(repository, settingsRepository = settingsRepository).test {
                var state = awaitItem()
                while (state.bandalartData?.id != 1L) state = awaitItem()

                assertTrue(state.showTaskCompletionTooltip)

                state.eventSink(HomeScreen.Event.DismissTaskCompletionTooltip)
                do {
                    state = awaitItem()
                } while (state.showTaskCompletionTooltip)

                assertFalse(state.showTaskCompletionTooltip)
                assertEquals(1, settingsRepository.taskCompletionTooltipDismissals)
                cancelAndIgnoreRemainingEvents()
            }

            presenter(repository, settingsRepository = settingsRepository).test {
                var state = awaitItem()
                while (state.bandalartData?.id != 1L) state = awaitItem()

                assertFalse(state.showTaskCompletionTooltip)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun routineSettingsOpenForTheCurrentBandalartAndDismissDiscovery() =
        runTest {
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1L), bandalart(2L, dailyResetEnabled = true)),
                    recentBandalartId = 2L,
                )
            val settingsRepository =
                FakeSettingsRepository(
                    initialRoutineSettingsTooltipDismissed = false,
                    initialTaskCompletionTooltipDismissed = false,
                )

            presenter(repository, settingsRepository = settingsRepository).test {
                var state = awaitItem()
                while (state.bandalartData?.id != 2L || !state.showRoutineSettingsTooltip) {
                    state = awaitItem()
                }

                assertFalse(state.showTaskCompletionTooltip)
                state.eventSink(HomeScreen.Event.OpenDropDownMenu)
                do {
                    state = awaitItem()
                } while (!state.isDropDownMenuOpened)

                assertFalse(state.showRoutineSettingsTooltip)
                assertEquals(1, settingsRepository.routineSettingsTooltipDismissals)

                state.eventSink(HomeScreen.Event.OpenRoutineSettings)
                do {
                    state = awaitItem()
                } while (state.bottomSheet !is HomeScreen.BottomSheetState.RoutineSettings)

                val sheet = state.bottomSheet
                assertEquals(2L, sheet.bandalartId)
                assertTrue(sheet.dailyResetEnabled)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun routineSettingsToggleUpdatesOnlyItsBandalart() =
        runTest {
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1L), bandalart(2L)),
                    recentBandalartId = 2L,
                )

            presenter(repository).test {
                var state = awaitItem()
                while (state.bandalartData?.id != 2L) state = awaitItem()

                state.eventSink(HomeScreen.Event.OpenRoutineSettings)
                do {
                    state = awaitItem()
                } while (state.bottomSheet !is HomeScreen.BottomSheetState.RoutineSettings)

                state.eventSink(HomeScreen.Event.SetDailyResetEnabled(bandalartId = 2L, enabled = true))
                do {
                    state = awaitItem()
                } while (
                    repository.dailyResetUpdates.isEmpty() ||
                    (state.bottomSheet as? HomeScreen.BottomSheetState.RoutineSettings)?.dailyResetEnabled != true
                )

                assertEquals(listOf(2L to true), repository.dailyResetUpdates)
                assertTrue(state.bandalartData?.dailyResetEnabled == true)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun resettingCompletionRequiresConfirmationAndReportsSuccess() =
        runTest {
            val mainCell = cell(id = 10L, parentId = null).copy(isCompleted = true)
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1L, isCompleted = true)),
                    recentBandalartId = 1L,
                    mainCells = mapOf(1L to mainCell),
                )

            presenter(repository).test {
                var state = awaitItem()
                while (state.bandalartData?.id != 1L) state = awaitItem()

                state.eventSink(HomeScreen.Event.OpenRoutineSettings)
                do {
                    state = awaitItem()
                } while (state.bottomSheet !is HomeScreen.BottomSheetState.RoutineSettings)
                assertTrue(state.bottomSheet.hasCompletedCells)

                state.eventSink(HomeScreen.Event.OpenResetCompletionsDialog)
                do {
                    state = awaitItem()
                } while (state.dialog !is HomeScreen.DialogState.ResetCompletions)

                val dialog = state.dialog
                assertEquals(1L, dialog.bandalartId)
                assertTrue(repository.completionResetIds.isEmpty())

                state.eventSink(HomeScreen.Event.ConfirmResetCompletions(dialog.bandalartId))
                do {
                    state = awaitItem()
                } while (state.effect != HomeScreen.Effect.ShowCompletionResetSnackbar)

                assertEquals(listOf(1L), repository.completionResetIds)
                assertNull(state.bottomSheet)
                assertNull(state.dialog)
                assertEquals(0, requireNotNull(state.bandalartData).completionRatio)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun dueDailyResetCheckRefreshesTheCurrentBandalart() =
        runTest {
            val mainCell = cell(id = 10L, parentId = null).copy(isCompleted = true)
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1L, isCompleted = true, dailyResetEnabled = true)),
                    recentBandalartId = 1L,
                    mainCells = mapOf(1L to mainCell),
                    dueDailyResetIds = setOf(1L),
                )

            presenter(repository).test {
                var state = awaitItem()
                while (state.bandalartData?.id != 1L) state = awaitItem()

                state.eventSink(HomeScreen.Event.CheckDueDailyResets)
                do {
                    state = awaitItem()
                } while (repository.dueDailyResetChecks == 0 || state.bandalartData?.completionRatio != 0)

                assertFalse(state.bandalartData.isCompleted)
                assertEquals(1, repository.dueDailyResetChecks)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun bandalart(
        id: Long,
        isCompleted: Boolean = false,
        dailyResetEnabled: Boolean = false,
    ) = BandalartEntity(
        id = id,
        mainColor = "#3FFFBA",
        subColor = "#111827",
        profileEmoji = "🎯",
        title = "반다라트 $id",
        description = "설명 $id",
        dueDate = null,
        isCompleted = isCompleted,
        completionRatio = if (isCompleted) 100 else 0,
        dailyResetEnabled = dailyResetEnabled,
    )

    private fun presenter(
        repository: FakeBandalartRepository,
        widgetLaunchTarget: BandalartWidgetLaunchTarget = BufferedBandalartWidgetLaunchTarget(),
        settingsRepository: FakeSettingsRepository = FakeSettingsRepository(),
    ) = HomePresenter(
        navigator = FakeNavigator(HomeScreen),
        bandalartRepository = repository,
        bandalartSlotRepository = FakeBandalartSlotRepository(),
        inAppUpdateRepository = FakeInAppUpdateRepository(),
        settingsRepository = settingsRepository,
        bandalartWidgetLaunchTarget = widgetLaunchTarget,
    )

    private fun cell(
        id: Long,
        parentId: Long?,
    ) = BandalartCellEntity(
        id = id,
        title = "셀 $id",
        description = "설명 $id",
        dueDate = null,
        isCompleted = false,
        parentId = parentId,
    )
}
