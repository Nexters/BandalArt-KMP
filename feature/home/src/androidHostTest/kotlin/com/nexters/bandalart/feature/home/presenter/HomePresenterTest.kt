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
import com.nexters.bandalart.feature.home.HomeScreen
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HomePresenterTest {
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
                while (state.bandalartData?.id != 2L || state.isLoading) {
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
                while (state.bandalartData == null || state.isLoading) {
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
                while (state.bandalartData?.id != created.id || state.isLoading) {
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
                while (state.bandalartData?.id != 2L || state.isLoading) {
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
                while (state.bandalartData?.id != 1L || state.isLoading) {
                    state = awaitItem()
                }

                state.eventSink(HomeScreen.Event.SelectBandalart(2L))
                do {
                    state = awaitItem()
                } while (state.bandalartData?.id != 2L || state.isLoading)

                assertEquals(2L, repository.recentBandalartId)
                assertFalse(state.isBandalartCompleted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun bandalart(
        id: Long,
        isCompleted: Boolean = false,
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
    )

    private fun presenter(repository: FakeBandalartRepository) =
        HomePresenter(
            navigator = FakeNavigator(HomeScreen),
            bandalartRepository = repository,
            bandalartSlotRepository = FakeBandalartSlotRepository(),
            inAppUpdateRepository = FakeInAppUpdateRepository(),
            settingsRepository = FakeSettingsRepository(),
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
