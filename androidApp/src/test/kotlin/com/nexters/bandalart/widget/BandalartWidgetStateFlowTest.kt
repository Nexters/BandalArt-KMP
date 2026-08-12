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

package com.nexters.bandalart.widget

import com.nexters.bandalart.core.domain.entity.BandalartWidgetSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BandalartWidgetStateFlowTest {
    @Test
    fun `active widget session renders latest progress after a database change`() =
        runBlocking {
            val changes = MutableSharedFlow<BandalartWidgetRecentSelection>(replay = 1)
            val firstStateCollected = CompletableDeferred<Unit>()
            var completionRatio = 10
            val renderedRatios = mutableListOf<Int>()
            val collection =
                launch {
                    observeWidgetViewStates(
                        configuredSelection = BandalartWidgetSelection(1L, 11L),
                        changes = changes,
                        loadAvailableSubGoalIds = { listOf(11L) },
                        loadSnapshot = { selection ->
                            widgetSnapshot(selection, completionRatio)
                        },
                        unnamedGoalTitle = "Untitled goal",
                    ).take(2).collect { state ->
                        renderedRatios += (state as BandalartWidgetViewState.Content).completionRatio
                        if (renderedRatios.size == 1) firstStateCollected.complete(Unit)
                    }
                }

            changes.emit(BandalartWidgetRecentSelection(bandalartId = 1L, subGoalId = 11L))
            firstStateCollected.await()
            completionRatio = 70
            changes.emit(BandalartWidgetRecentSelection(bandalartId = 1L, subGoalId = 11L))
            collection.join()

            assertEquals(listOf(10, 70), renderedRatios)
        }

    @Test
    fun `recent board and subgoal are resolved from one emitted selection`() =
        runBlocking {
            val changes = MutableSharedFlow<BandalartWidgetRecentSelection>(replay = 1)
            val loadedSelections = mutableListOf<BandalartWidgetSelection>()
            val collection =
                launch {
                    observeWidgetViewStates(
                        configuredSelection = BandalartWidgetSelection(1L, 11L),
                        changes = changes,
                        loadAvailableSubGoalIds = { bandalartId ->
                            if (bandalartId == 2L) listOf(21L) else listOf(11L)
                        },
                        loadSnapshot = { selection ->
                            loadedSelections += selection
                            widgetSnapshot(selection, completionRatio = 30)
                        },
                        unnamedGoalTitle = "Untitled goal",
                    ).first()
                }

            changes.emit(BandalartWidgetRecentSelection(bandalartId = 2L, subGoalId = 21L))
            collection.join()

            assertEquals(listOf(BandalartWidgetSelection(2L, 21L)), loadedSelections)
        }

    private fun widgetSnapshot(
        selection: BandalartWidgetSelection,
        completionRatio: Int,
    ) = BandalartWidgetSnapshot(
        bandalartId = selection.bandalartId,
        subGoalId = selection.subGoalId,
        title = "Goal",
        profileEmoji = "🎯",
        completionRatio = completionRatio,
        subGoalTitle = "Sub goal",
        tasks = emptyList(),
    )
}
