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

import kotlin.test.Test
import kotlin.test.assertEquals

class IosWidgetMappingTest {
    @Test
    fun `board options expose only persisted nonblank boards`() {
        val options =
            listOf(
                IosWidgetBandalartRecord(id = 1L, title = "건강", profileEmoji = "💪"),
                IosWidgetBandalartRecord(id = 2L, title = " ", profileEmoji = null),
                IosWidgetBandalartRecord(id = null, title = "미저장", profileEmoji = null),
            ).toIosWidgetBandalartOptions()

        assertEquals(
            listOf(IosWidgetBandalartOption(id = 1L, title = "건강", profileEmoji = "💪")),
            options,
        )
    }

    @Test
    fun `subgoal options expose only owned nonblank children`() {
        val options =
            listOf(
                IosWidgetSubGoalRecord(id = 11L, bandalartId = 1L, parentId = 10L, title = "운동"),
                IosWidgetSubGoalRecord(id = 12L, bandalartId = 1L, parentId = 10L, title = " "),
                IosWidgetSubGoalRecord(id = 13L, bandalartId = 2L, parentId = 10L, title = "다른 표"),
                IosWidgetSubGoalRecord(id = 14L, bandalartId = 1L, parentId = 99L, title = "다른 부모"),
            ).toIosWidgetSubGoalOptions(bandalartId = 1L, mainCellId = 10L)

        assertEquals(
            listOf(IosWidgetSubGoalOption(id = 11L, bandalartId = 1L, title = "운동")),
            options,
        )
    }

    @Test
    fun `snapshot mapping excludes tasks without persisted nonblank content`() {
        val snapshot =
            IosWidgetSnapshotRecord(
                bandalartId = 1L,
                subGoalId = 11L,
                title = "건강",
                profileEmoji = "💪",
                completionRatio = 40,
                subGoalTitle = "운동",
                tasks =
                    listOf(
                        IosWidgetTaskRecord(id = 21L, title = "달리기", isCompleted = true),
                        IosWidgetTaskRecord(id = 22L, title = " ", isCompleted = false),
                        IosWidgetTaskRecord(id = null, title = "미저장", isCompleted = false),
                    ),
            ).toIosWidgetSnapshot()

        assertEquals(
            IosWidgetSnapshot(
                bandalartId = 1L,
                subGoalId = 11L,
                title = "건강",
                profileEmoji = "💪",
                completionRatio = 40,
                subGoalTitle = "운동",
                tasks = listOf(IosWidgetTask(id = 21L, title = "달리기", isCompleted = true)),
            ),
            snapshot,
        )
    }
}
