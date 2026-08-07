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
import com.nexters.bandalart.core.common.Language
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.home.model.CellType
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HomePresenterEditTest {
    @Test
    fun addingBandalartSelectsCreatedItemAndClosesListSheet() =
        runTest {
            val created = bandalart(2L)
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1L)),
                    recentBandalartId = 1L,
                    createdBandalart = created,
                )
            val presenter = presenter(repository)

            presenter.test {
                var state = awaitLoadedBandalart(1L)
                state.eventSink(HomeScreen.Event.OpenBandalartList)
                state = awaitItem()
                assertInstanceOf(HomeScreen.BottomSheetState.BandalartList::class.java, state.bottomSheet)

                state.eventSink(HomeScreen.Event.AddBandalart)
                do {
                    state = awaitItem()
                } while (
                    state.bandalartData?.id != created.id ||
                    state.effect != HomeScreen.Effect.ShowCreateSnackbar
                )

                assertEquals(1, repository.createCalls)
                assertEquals(created.id, repository.recentBandalartId)
                assertTrue(repository.completionUpdates.contains(created.id to false))
                assertNull(state.bottomSheet)
            }
        }

    @Test
    fun sixthBandalartIsRejectedWithoutRepositoryCall() =
        runTest {
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = List(5) { index -> bandalart(index + 1L) },
                    recentBandalartId = 1L,
                )
            val presenter = presenter(repository)

            presenter.test {
                var state = awaitLoadedBandalart(1L)
                state.eventSink(HomeScreen.Event.AddBandalart)
                do {
                    state = awaitItem()
                } while (state.effect != HomeScreen.Effect.ShowLimitToast)

                assertEquals(0, repository.createCalls)
                state.eventSink(HomeScreen.Event.ConsumeEffect)
                do {
                    state = awaitItem()
                } while (state.effect != null)
                assertNull(state.effect)
            }
        }

    @Test
    fun mainCellDraftIsValidatedAndSaved() =
        runTest {
            val mainCell = cell(id = 10L, title = "기존 목표")
            val repository = repositoryWithCells(mainCell = mainCell)
            val presenter = presenter(repository)

            presenter.test {
                var state = awaitLoadedBandalart(1L)
                state.eventSink(
                    HomeScreen.Event.OpenCell(
                        cellType = CellType.MAIN,
                        isMainCellTitleEmpty = false,
                        cellData = mainCell,
                    ),
                )
                state = awaitCellSheet()

                state.eventSink(HomeScreen.Event.UpdateCellTitle("가".repeat(16), Language.KOREAN))
                expectNoEvents()
                assertEquals("기존 목표", state.cellSheet().cellData.title)

                state.eventSink(HomeScreen.Event.UpdateCellTitle("  새 목표  ", Language.KOREAN))
                state.eventSink(HomeScreen.Event.UpdateDescription("새 설명"))
                state.eventSink(HomeScreen.Event.OpenDatePicker)
                state.eventSink(HomeScreen.Event.UpdateDueDate(""))
                state.eventSink(HomeScreen.Event.OpenEmojiPicker)
                state.eventSink(HomeScreen.Event.UpdateEmojiDraft("🚀"))
                state.eventSink(HomeScreen.Event.UpdateThemeColor("#ABCDEF", "#123456"))
                state.eventSink(HomeScreen.Event.SaveCell)
                do {
                    state = awaitItem()
                } while (repository.mainCellUpdate == null || state.bottomSheet != null)

                val update = requireNotNull(repository.mainCellUpdate)
                assertEquals(1L, update.bandalartId)
                assertEquals(mainCell.id, update.cellId)
                assertEquals("새 목표", update.entity.title)
                assertEquals("새 설명", update.entity.description)
                assertNull(update.entity.dueDate)
                assertEquals("🚀", update.entity.profileEmoji)
                assertEquals("#ABCDEF", update.entity.mainColor)
                assertEquals("#123456", update.entity.subColor)
            }
        }

    @Test
    fun subAndTaskCellDraftsUseTheirRepositoryPayloads() =
        runTest {
            val mainCell = cell(id = 10L, title = "메인")
            val subCell = cell(id = 11L, title = "서브", parentId = mainCell.id)
            val taskCell = cell(id = 12L, title = "태스크", parentId = subCell.id)
            val repository =
                repositoryWithCells(
                    mainCell = mainCell,
                    childCells =
                        mapOf(
                            mainCell.id to listOf(subCell),
                            subCell.id to listOf(taskCell),
                        ),
                )
            val presenter = presenter(repository)

            presenter.test {
                var state = awaitLoadedBandalart(1L)
                state.eventSink(HomeScreen.Event.OpenCell(CellType.SUB, false, subCell))
                state = awaitCellSheet()
                state.eventSink(HomeScreen.Event.UpdateCellTitle("서브 수정", Language.KOREAN))
                state.eventSink(HomeScreen.Event.UpdateDescription("서브 설명"))
                state.eventSink(HomeScreen.Event.UpdateDueDate("2026-08-05"))
                state.eventSink(HomeScreen.Event.SaveCell)
                do {
                    state = awaitItem()
                } while (repository.subCellUpdate == null || state.bottomSheet != null)

                val subUpdate = requireNotNull(repository.subCellUpdate)
                assertEquals(subCell.id, subUpdate.cellId)
                assertEquals("서브 수정", subUpdate.entity.title)
                assertEquals("서브 설명", subUpdate.entity.description)
                assertEquals("2026-08-05", subUpdate.entity.dueDate)

                state.eventSink(HomeScreen.Event.OpenCell(CellType.TASK, false, taskCell))
                state = awaitCellSheet()
                state.eventSink(HomeScreen.Event.UpdateCompletion(true))
                state.eventSink(HomeScreen.Event.UpdateDescription("태스크 설명"))
                state.eventSink(HomeScreen.Event.SaveCell)
                do {
                    state = awaitItem()
                } while (repository.taskCellUpdate == null || state.bottomSheet != null)

                val taskUpdate = requireNotNull(repository.taskCellUpdate)
                assertEquals(taskCell.id, taskUpdate.cellId)
                assertEquals("태스크 설명", taskUpdate.entity.description)
                assertTrue(taskUpdate.entity.isCompleted == true)
            }
        }

    @Test
    fun cellDeleteUsesDialogTargetAndClosesModal() =
        runTest {
            val mainCell = cell(id = 10L, title = "메인")
            val taskCell = cell(id = 12L, title = "삭제 대상", parentId = mainCell.id)
            val repository = repositoryWithCells(mainCell = mainCell)
            val presenter = presenter(repository)

            presenter.test {
                var state = awaitLoadedBandalart(1L)
                state.eventSink(HomeScreen.Event.OpenCell(CellType.TASK, false, taskCell))
                state = awaitCellSheet()
                state.eventSink(HomeScreen.Event.OpenCellDeleteDialog)
                do {
                    state = awaitItem()
                } while (state.dialog !is HomeScreen.DialogState.CellDelete)

                val dialog = state.dialog as HomeScreen.DialogState.CellDelete
                assertEquals(taskCell.id, dialog.cellId)
                assertEquals(CellType.TASK, dialog.cellType)
                assertEquals(taskCell.title, dialog.cellTitle)

                state.eventSink(HomeScreen.Event.DeleteCell(dialog.cellId))
                do {
                    state = awaitItem()
                } while (repository.deletedCellIds.isEmpty() || state.dialog != null || state.bottomSheet != null)

                assertEquals(listOf(taskCell.id), repository.deletedCellIds)
            }
        }

    @Test
    fun bandalartDeleteAlsoClearsCompletionSnapshotAndDropDown() =
        runTest {
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1L), bandalart(2L)),
                    recentBandalartId = 1L,
                )
            val presenter = presenter(repository)

            presenter.test {
                var state = awaitLoadedBandalart(1L)
                state.eventSink(HomeScreen.Event.OpenDropDownMenu)
                state = awaitItem()
                assertTrue(state.isDropDownMenuOpened)
                state.eventSink(HomeScreen.Event.OpenBandalartDeleteDialog)
                state = awaitItem()
                assertEquals(HomeScreen.DialogState.BandalartDelete, state.dialog)

                state.eventSink(HomeScreen.Event.DeleteBandalart(1L))
                do {
                    state = awaitItem()
                } while (
                    repository.deletedBandalartIds.isEmpty() ||
                    state.effect != HomeScreen.Effect.ShowDeleteSnackbar
                )

                assertEquals(listOf(1L), repository.deletedBandalartIds)
                assertEquals(listOf(1L), repository.deletedCompletionIds)
                assertFalse(state.isDropDownMenuOpened)
                assertNull(state.dialog)
                assertNull(state.bottomSheet)
            }
        }

    @Test
    fun invalidCellEntryAndQuickEmojiUpdateAreHandled() =
        runTest {
            val mainCell = cell(id = 10L, title = null)
            val subCell = cell(id = 11L, title = "서브", parentId = mainCell.id)
            val repository = repositoryWithCells(mainCell = mainCell)
            val presenter = presenter(repository)

            presenter.test {
                var state = awaitLoadedBandalart(1L)
                state.eventSink(HomeScreen.Event.OpenCell(CellType.SUB, true, subCell))
                do {
                    state = awaitItem()
                } while (state.effect != HomeScreen.Effect.ShowMainGoalToast)
                assertNull(state.bottomSheet)

                state.eventSink(HomeScreen.Event.ConsumeEffect)
                state.eventSink(HomeScreen.Event.OpenEmoji)
                do {
                    state = awaitItem()
                } while (state.bottomSheet !is HomeScreen.BottomSheetState.Emoji)

                state.eventSink(HomeScreen.Event.UpdateBandalartEmoji(1L, mainCell.id, "🌟"))
                state.eventSink(HomeScreen.Event.UpdateBandalartEmoji(1L, mainCell.id, "🚀"))
                do {
                    state = awaitItem()
                } while (repository.emojiUpdates.isEmpty() || state.bottomSheet != null)

                val update = repository.emojiUpdates.single()
                assertEquals(1L, update.bandalartId)
                assertEquals(mainCell.id, update.cellId)
                assertEquals("🌟", update.entity.profileEmoji)
            }
        }

    @Test
    fun closingEmojiPickerKeepsCellDraftOpen() =
        runTest {
            val mainCell = cell(id = 10L, title = "기존 목표")
            val repository = repositoryWithCells(mainCell = mainCell)
            val presenter = presenter(repository)

            presenter.test {
                var state = awaitLoadedBandalart(1L)
                state.eventSink(HomeScreen.Event.OpenCell(CellType.MAIN, false, mainCell))
                state = awaitCellSheet()

                state.eventSink(HomeScreen.Event.OpenEmojiPicker)
                do {
                    state = awaitItem()
                } while (!state.cellSheet().isEmojiPickerOpened)

                state.eventSink(HomeScreen.Event.CloseEmojiPicker)
                do {
                    state = awaitItem()
                } while (state.cellSheet().isEmojiPickerOpened)

                assertInstanceOf(HomeScreen.BottomSheetState.Cell::class.java, state.bottomSheet)
                assertEquals("기존 목표", state.cellSheet().cellData.title)
            }
        }

    private fun repositoryWithCells(
        mainCell: BandalartCellEntity,
        childCells: Map<Long, List<BandalartCellEntity>> = emptyMap(),
    ) = FakeBandalartRepository(
        initialBandalarts = listOf(bandalart(1L)),
        recentBandalartId = 1L,
        mainCells = mapOf(1L to mainCell),
        childCells = childCells,
    )

    private fun presenter(repository: FakeBandalartRepository) =
        HomePresenter(
            navigator = FakeNavigator(HomeScreen),
            bandalartRepository = repository,
            inAppUpdateRepository = FakeInAppUpdateRepository(),
            settingsRepository = FakeSettingsRepository(),
        )

    private suspend fun ReceiveTurbine<HomeScreen.State>.awaitLoadedBandalart(bandalartId: Long,): HomeScreen.State {
        var state = awaitItem()
        while (state.bandalartData?.id != bandalartId || state.isLoading) {
            state = awaitItem()
        }
        return state
    }

    private suspend fun ReceiveTurbine<HomeScreen.State>.awaitCellSheet(): HomeScreen.State {
        var state = awaitItem()
        while (state.bottomSheet !is HomeScreen.BottomSheetState.Cell) {
            state = awaitItem()
        }
        return state
    }

    private fun HomeScreen.State.cellSheet(): HomeScreen.BottomSheetState.Cell = requireNotNull(bottomSheet as? HomeScreen.BottomSheetState.Cell)

    private fun bandalart(id: Long) =
        BandalartEntity(
            id = id,
            mainColor = "#3FFFBA",
            subColor = "#111827",
            profileEmoji = "🎯",
            title = "반다라트 $id",
            description = "설명 $id",
            dueDate = null,
            isCompleted = false,
            completionRatio = 0,
        )

    private fun cell(
        id: Long,
        title: String?,
        parentId: Long? = null,
    ) = BandalartCellEntity(
        id = id,
        title = title,
        description = null,
        dueDate = null,
        isCompleted = false,
        parentId = parentId,
    )
}
