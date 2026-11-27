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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomePresenterWeeklyFocusTest {
    private val run = cell(id = 21L, title = "달리기")
    private val squat = cell(id = 22L, title = "스쿼트", isCompleted = true)
    private val stretch = cell(id = 23L, title = "스트레칭")
    private val read = cell(id = 24L, title = "책 읽기")
    private val walk = cell(id = 25L, title = "걷기")
    private val blank = cell(id = 26L, title = null)

    @Test
    fun selectionSheetListsIncompleteTitledTasksAndSavesToHomeCard() =
        runTest {
            val repository = repository()

            presenter(repository).test {
                var state = awaitLoadedBandalart()
                assertTrue(state.weeklyFocusTasks.isEmpty())

                state.eventSink(HomeScreen.Event.OpenWeeklyFocusSelection)
                state = awaitState { it.focusSheet() != null }
                val sheet = state.focusSheet()!!
                assertEquals(listOf(21L, 23L, 24L, 25L), sheet.candidates.map { it.cell.id })
                assertTrue(sheet.selectedIds.isEmpty())

                state.eventSink(HomeScreen.Event.ToggleWeeklyFocusSelection(23L))
                state = awaitState { it.focusSheet()?.selectedIds == listOf(23L) }
                state.eventSink(HomeScreen.Event.ToggleWeeklyFocusSelection(21L))
                state = awaitState { it.focusSheet()?.selectedIds == listOf(23L, 21L) }
                state.eventSink(HomeScreen.Event.SaveWeeklyFocusSelection)
                state = awaitState { it.bottomSheet == null && it.weeklyFocusTasks.isNotEmpty() }

                assertEquals(listOf(23L, 21L), state.weeklyFocusTasks.map { it.cell.id })
                assertEquals(listOf(23L, 21L), repository.weeklyFocusTaskIds[1L])
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun fourthTaskIsIgnoredAtLimitAndDeselectingFreesSlot() =
        runTest {
            presenter(repository()).test {
                var state = awaitLoadedBandalart()
                state.eventSink(HomeScreen.Event.OpenWeeklyFocusSelection)
                state = awaitState { it.focusSheet() != null }
                listOf(21L, 23L, 24L).forEach { id ->
                    state.eventSink(HomeScreen.Event.ToggleWeeklyFocusSelection(id))
                    state = awaitState { it.focusSheet()?.selectedIds?.contains(id) == true }
                }

                state.eventSink(HomeScreen.Event.ToggleWeeklyFocusSelection(25L))
                state.eventSink(HomeScreen.Event.ToggleWeeklyFocusSelection(23L))
                state = awaitState { it.focusSheet()?.selectedIds?.contains(23L) == false }
                assertEquals(listOf(21L, 24L), state.focusSheet()?.selectedIds)

                state.eventSink(HomeScreen.Event.ToggleWeeklyFocusSelection(25L))
                state = awaitState { it.focusSheet()?.selectedIds?.size == 3 }
                assertEquals(listOf(21L, 24L, 25L), state.focusSheet()?.selectedIds)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun dismissingWithoutSaveKeepsExistingSelection() =
        runTest {
            val repository = repository(selectedIds = listOf(21L))

            presenter(repository).test {
                var state = awaitState { it.weeklyFocusTasks.isNotEmpty() }
                state.eventSink(HomeScreen.Event.OpenWeeklyFocusSelection)
                state = awaitState { it.focusSheet()?.selectedIds == listOf(21L) }
                state.eventSink(HomeScreen.Event.ClearWeeklyFocusSelection)
                state = awaitState { it.focusSheet()?.selectedIds?.isEmpty() == true }
                state.eventSink(HomeScreen.Event.DismissBottomSheet)
                state = awaitState { it.bottomSheet == null }

                assertEquals(listOf(21L), state.weeklyFocusTasks.map { it.cell.id })
                assertEquals(listOf(21L), repository.weeklyFocusTaskIds[1L])
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun completedSelectedTaskStaysDeselectableAndBlankTaskIsHidden() =
        runTest {
            val repository = repository(selectedIds = listOf(22L, 26L))

            presenter(repository).test {
                var state = awaitState { it.weeklyFocusTasks.isNotEmpty() }
                assertEquals(listOf(22L), state.weeklyFocusTasks.map { it.cell.id })

                state.eventSink(HomeScreen.Event.OpenWeeklyFocusSelection)
                val sheet = awaitState { it.focusSheet() != null }.focusSheet()!!
                assertEquals(listOf(22L), sheet.selectedIds)
                assertTrue(sheet.candidates.any { it.cell.id == 22L })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun checkingFromCardCompletesOriginalCell() =
        runTest {
            val repository = repository(selectedIds = listOf(21L))

            presenter(repository).test {
                val state = awaitState { it.weeklyFocusTasks.isNotEmpty() }
                state.eventSink(HomeScreen.Event.ToggleTaskCompletion(state.weeklyFocusTasks.single().cell))
                val updated =
                    awaitState {
                        it.weeklyFocusTasks
                            .singleOrNull()
                            ?.cell
                            ?.isCompleted == true
                    }

                assertEquals(21L, repository.taskCellUpdate?.cellId)
                assertTrue(updated.taskCell(21L).isCompleted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun newWeekClearsCardOnForegroundCheck() =
        runTest {
            val repository = repository(selectedIds = listOf(21L))

            presenter(repository).test {
                val state = awaitState { it.weeklyFocusTasks.isNotEmpty() }
                // 저장소는 지난주 선택을 빈 목록으로 돌려준다.
                repository.weeklyFocusTaskIds.clear()
                state.eventSink(HomeScreen.Event.CheckDueDailyResets)
                val cleared = awaitState { it.weeklyFocusTasks.isEmpty() }

                assertNull(cleared.bottomSheet)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun switchingBandalartShowsThatBoardsSelection() =
        runTest {
            val repository =
                repository(
                    selectedIds = listOf(21L),
                    extraBandalart = true,
                )

            presenter(repository).test {
                var state = awaitState { it.weeklyFocusTasks.isNotEmpty() }
                state.eventSink(HomeScreen.Event.SelectBandalart(2L))
                state = awaitLoadedBandalart(bandalartId = 2L)

                assertTrue(state.weeklyFocusTasks.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun repository(
        selectedIds: List<Long> = emptyList(),
        extraBandalart: Boolean = false,
    ): FakeBandalartRepository {
        val mainCell = cell(id = 10L, title = "메인", parentId = null)
        val subCell = cell(id = 11L, title = "운동", parentId = mainCell.id)
        val otherMainCell = cell(id = 30L, title = "다른 메인", parentId = null)
        val bandalarts = if (extraBandalart) listOf(bandalart(1L), bandalart(2L)) else listOf(bandalart(1L))
        return FakeBandalartRepository(
            initialBandalarts = bandalarts,
            recentBandalartId = 1L,
            mainCells = mapOf(1L to mainCell, 2L to otherMainCell),
            childCells =
                mapOf(
                    mainCell.id to listOf(subCell),
                    subCell.id to listOf(run, squat, stretch, read, walk, blank),
                ),
        ).also { if (selectedIds.isNotEmpty()) it.weeklyFocusTaskIds[1L] = selectedIds }
    }

    private fun presenter(repository: FakeBandalartRepository) =
        HomePresenter(
            navigator = FakeNavigator(HomeScreen),
            bandalartRepository = repository,
            bandalartSlotRepository = FakeBandalartSlotRepository(),
            inAppUpdateRepository = FakeInAppUpdateRepository(),
            settingsRepository = FakeSettingsRepository(initialTaskCompletionTooltipDismissed = true),
        )

    private suspend fun ReceiveTurbine<HomeScreen.State>.awaitState(predicate: (HomeScreen.State) -> Boolean): HomeScreen.State {
        var state = awaitItem()
        while (!predicate(state)) state = awaitItem()
        return state
    }

    private suspend fun ReceiveTurbine<HomeScreen.State>.awaitLoadedBandalart(bandalartId: Long = 1L): HomeScreen.State =
        awaitState { it.bandalartData?.id == bandalartId }

    private fun HomeScreen.State.focusSheet() = bottomSheet as? HomeScreen.BottomSheetState.WeeklyFocusSelection

    private fun HomeScreen.State.taskCell(cellId: Long): BandalartCellEntity =
        requireNotNull(
            bandalartCellData
                ?.children
                ?.flatMap { subCell -> subCell.children }
                ?.firstOrNull { taskCell -> taskCell.id == cellId },
        )

    private fun bandalart(id: Long) =
        BandalartEntity(
            id = id,
            mainColor = "#3FFFBA",
            subColor = "#111827",
            profileEmoji = "🎯",
            title = "반다라트 $id",
            description = null,
            dueDate = null,
            isCompleted = false,
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
        isCompleted = isCompleted,
        parentId = parentId,
    )
}
