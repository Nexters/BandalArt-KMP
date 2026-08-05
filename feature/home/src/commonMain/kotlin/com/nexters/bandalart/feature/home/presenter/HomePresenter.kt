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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.home.mapper.toUiModel
import com.nexters.bandalart.feature.home.model.BandalartUiModel
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch

@AssistedInject
class HomePresenter(
    @Suppress("UnusedPrivateProperty")
    @Assisted navigator: Navigator,
    private val bandalartRepository: BandalartRepository,
) : Presenter<HomeScreen.State> {
    @Composable
    override fun present(): HomeScreen.State {
        var bandalartList by remember { mutableStateOf(persistentListOf<BandalartUiModel>()) }
        var bandalartData by remember { mutableStateOf<BandalartUiModel?>(null) }
        var bandalartCellData by remember { mutableStateOf<BandalartCellEntity?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var isBandalartCompleted by remember { mutableStateOf(false) }
        var isCreatingEmptyBandalart by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        suspend fun loadBandalart(
            bandalartId: Long,
            isCompleted: Boolean = false,
        ) {
            isLoading = true
            bandalartData = bandalartRepository.getBandalart(bandalartId).toUiModel()

            val mainCell = bandalartRepository.getBandalartMainCell(bandalartId)
            val subCells = bandalartRepository.getChildCells(mainCell?.id ?: 0L)
            val children =
                subCells.map { subCell ->
                    val taskCells = bandalartRepository.getChildCells(subCell.id)
                    BandalartCellEntity(
                        id = subCell.id,
                        title = subCell.title,
                        description = subCell.description,
                        dueDate = subCell.dueDate,
                        isCompleted = subCell.isCompleted,
                        parentId = subCell.parentId,
                        children =
                            taskCells.map { taskCell ->
                                BandalartCellEntity(
                                    id = taskCell.id,
                                    title = taskCell.title,
                                    description = taskCell.description,
                                    dueDate = taskCell.dueDate,
                                    isCompleted = taskCell.isCompleted,
                                    parentId = taskCell.parentId,
                                    children = emptyList(),
                                )
                            },
                    )
                }

            bandalartCellData =
                BandalartCellEntity(
                    id = mainCell?.id ?: 0L,
                    title = mainCell?.title,
                    description = mainCell?.description,
                    dueDate = mainCell?.dueDate,
                    isCompleted = mainCell?.isCompleted ?: false,
                    parentId = mainCell?.parentId,
                    children = children,
                )
            isBandalartCompleted = isCompleted
            isLoading = false
        }

        suspend fun createInitialBandalart() {
            if (isCreatingEmptyBandalart) return

            isCreatingEmptyBandalart = true
            bandalartRepository.createBandalart()?.let { bandalart ->
                bandalartRepository.setRecentBandalartId(bandalart.id)
                bandalartRepository.upsertBandalartId(bandalart.id, bandalart.isCompleted)
                loadBandalart(bandalart.id)
            }
        }

        LaunchedEffect(Unit) {
            bandalartRepository.getBandalartList().collect { entities ->
                val currentList = entities.map { it.toUiModel() }
                bandalartList = currentList.toPersistentList()

                val previousList = bandalartRepository.getPrevBandalartList()
                val newlyCompletedIds =
                    currentList
                        .filter { bandalart ->
                            val previous = previousList.find { it.first == bandalart.id }
                            previous != null && !previous.second && bandalart.isCompleted
                        }.map { it.id }

                if (newlyCompletedIds.isNotEmpty()) {
                    loadBandalart(
                        bandalartId = newlyCompletedIds.first(),
                        isCompleted = true,
                    )
                    return@collect
                }

                currentList.forEach { bandalart ->
                    bandalartRepository.upsertBandalartId(
                        bandalartId = bandalart.id,
                        isCompleted = bandalart.isCompleted,
                    )
                }

                if (currentList.isEmpty()) {
                    createInitialBandalart()
                    return@collect
                }

                isCreatingEmptyBandalart = false
                val recentBandalartId = bandalartRepository.getRecentBandalartId()
                val selectedId =
                    recentBandalartId.takeIf { recentId ->
                        currentList.any { it.id == recentId }
                    } ?: currentList.first().id
                loadBandalart(selectedId)
            }
        }

        return HomeScreen.State(
            bandalartList = bandalartList,
            bandalartData = bandalartData,
            bandalartCellData = bandalartCellData,
            isLoading = isLoading,
            isBandalartCompleted = isBandalartCompleted,
        ) { event ->
            when (event) {
                is HomeScreen.Event.SelectBandalart -> {
                    scope.launch {
                        bandalartRepository.setRecentBandalartId(event.bandalartId)
                        loadBandalart(event.bandalartId)
                    }
                }
            }
        }
    }

    @CircuitInject(HomeScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(
            @Assisted navigator: Navigator,
        ): HomePresenter
    }
}
