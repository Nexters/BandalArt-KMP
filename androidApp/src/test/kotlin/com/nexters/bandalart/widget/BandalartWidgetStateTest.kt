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

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.nexters.bandalart.core.domain.entity.BandalartWidgetSnapshot
import com.nexters.bandalart.core.domain.entity.BandalartWidgetTask
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BandalartWidgetStateTest {
    @Test
    fun `last viewed board replaces configured board and clears its subgoal`() {
        assertEquals(
            BandalartWidgetSelection(bandalartId = 3L, subGoalId = null),
            resolveWidgetSelection(
                configuredSelection = BandalartWidgetSelection(bandalartId = 1L, subGoalId = 2L),
                recentBandalartId = 3L,
            ),
        )
    }

    @Test
    fun `last viewed board keeps configured subgoal when board is unchanged`() {
        assertEquals(
            BandalartWidgetSelection(bandalartId = 1L, subGoalId = 2L),
            resolveWidgetSelection(
                configuredSelection = BandalartWidgetSelection(bandalartId = 1L, subGoalId = 2L),
                recentBandalartId = 1L,
            ),
        )
    }

    @Test
    fun `configured board remains the fallback before a board has been viewed`() {
        assertEquals(
            BandalartWidgetSelection(bandalartId = 1L, subGoalId = 2L),
            resolveWidgetSelection(
                configuredSelection = BandalartWidgetSelection(bandalartId = 1L, subGoalId = 2L),
                recentBandalartId = 0L,
            ),
        )
    }

    @Test
    fun `changing the configured board clears its subgoal selection`() {
        assertEquals(
            null,
            subGoalIdAfterBandalartSelection(
                currentBandalartId = 1L,
                currentSubGoalId = 2L,
                selectedBandalartId = 3L,
            ),
        )
        assertEquals(
            2L,
            subGoalIdAfterBandalartSelection(
                currentBandalartId = 1L,
                currentSubGoalId = 2L,
                selectedBandalartId = 1L,
            ),
        )
    }

    @Test
    fun `saving a board without a subgoal removes the previous subgoal key`() {
        val preferences = mutablePreferencesOf(BandalartIdKey to 1L, SubGoalIdKey to 2L)

        preferences.setWidgetSelection(bandalartId = 3L, subGoalId = null)

        assertEquals(BandalartWidgetSelection(3L, null), preferences.toWidgetSelection())
    }

    @Test
    fun `maps configured missing data to deleted and absent selection to unconfigured`() {
        assertEquals(BandalartWidgetViewState.Unconfigured, toWidgetViewState(null, null, "Untitled goal"))
        assertEquals(
            BandalartWidgetViewState.Deleted,
            toWidgetViewState(BandalartWidgetSelection(1L, 2L), null, "Untitled goal"),
        )
    }

    @Test
    fun `normalizes content and keeps at most five titled tasks`() {
        val state =
            toWidgetViewState(
                selection = BandalartWidgetSelection(1L, 2L),
                unnamedGoalTitle = "Untitled goal",
                snapshot =
                    BandalartWidgetSnapshot(
                        bandalartId = 1L,
                        subGoalId = 2L,
                        title = "",
                        profileEmoji = null,
                        completionRatio = 120,
                        subGoalTitle = "세부 목표",
                        tasks =
                            listOf(
                                BandalartWidgetTask(1L, "첫 번째", false),
                                BandalartWidgetTask(2L, " ", false),
                                BandalartWidgetTask(3L, "세 번째", true),
                                BandalartWidgetTask(4L, "네 번째", false),
                                BandalartWidgetTask(5L, "다섯 번째", false),
                                BandalartWidgetTask(6L, "여섯 번째", false),
                                BandalartWidgetTask(7L, "일곱 번째", false),
                            ),
                    ),
            ) as BandalartWidgetViewState.Content

        assertEquals("Untitled goal", state.title)
        assertEquals("🎯", state.profileEmoji)
        assertEquals(100, state.completionRatio)
        assertEquals(2L, state.subGoalId)
        assertEquals(5, state.tasks.size)
        assertEquals(2, state.tasksFor(BandalartWidgetLayout.MEDIUM).size)
        assertEquals(5, state.tasksFor(BandalartWidgetLayout.LARGE).size)
    }

    @Test
    fun `resolves responsive breakpoints`() {
        assertEquals(BandalartWidgetLayout.SMALL, resolveWidgetLayout(widthDp = 110, heightDp = 110))
        assertEquals(BandalartWidgetLayout.SMALL, resolveWidgetLayout(widthDp = 120, heightDp = 120))
        assertEquals(BandalartWidgetLayout.MEDIUM, resolveWidgetLayout(widthDp = 220, heightDp = 110))
        assertEquals(BandalartWidgetLayout.MEDIUM, resolveWidgetLayout(widthDp = 250, heightDp = 120))
        assertEquals(BandalartWidgetLayout.LARGE, resolveWidgetLayout(widthDp = 250, heightDp = 240))
    }
}
