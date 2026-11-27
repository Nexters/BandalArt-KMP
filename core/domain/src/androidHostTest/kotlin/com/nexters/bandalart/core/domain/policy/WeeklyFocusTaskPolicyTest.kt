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
package com.nexters.bandalart.core.domain.policy

import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WeeklyFocusTaskPolicyTest {
    private val mainCell =
        BandalartCellEntity(
            id = 1L,
            title = "메인",
            parentId = null,
            children =
                listOf(
                    BandalartCellEntity(
                        id = 10L,
                        title = "운동",
                        parentId = 1L,
                        children =
                            listOf(
                                task(id = 11L, title = "달리기", parentId = 10L),
                                task(id = 12L, title = "스쿼트", parentId = 10L, isCompleted = true),
                                task(id = 13L, title = null, parentId = 10L),
                            ),
                    ),
                    BandalartCellEntity(
                        id = 20L,
                        title = "공부",
                        parentId = 1L,
                        children =
                            listOf(
                                task(id = 21L, title = "단어", parentId = 20L),
                                task(id = 22L, title = " ", parentId = 20L),
                            ),
                    ),
                ),
        )

    @Test
    fun focusTasksKeepSelectionOrderAndDropBlankOrUnknownCells() {
        val tasks = weeklyFocusTasksOf(mainCell, listOf(21L, 13L, 11L, 999L, 21L))

        assertEquals(listOf(21L, 11L), tasks.map { it.cell.id })
        assertEquals(listOf("공부", "운동"), tasks.map { it.subGoalTitle })
    }

    @Test
    fun subGoalCellsAreNeverFocusTasks() {
        assertEquals(emptyList<WeeklyFocusTask>(), weeklyFocusTasksOf(mainCell, listOf(10L, 20L)))
    }

    @Test
    fun candidatesAreIncompleteTitledTasksInBoardOrder() {
        val candidates = weeklyFocusCandidatesOf(mainCell, selectedIds = emptyList())

        assertEquals(listOf(11L, 21L), candidates.map { it.cell.id })
    }

    @Test
    fun completedTaskStaysCandidateWhileSelected() {
        val candidates = weeklyFocusCandidatesOf(mainCell, selectedIds = listOf(12L))

        assertEquals(listOf(11L, 12L, 21L), candidates.map { it.cell.id })
    }

    private fun task(
        id: Long,
        title: String?,
        parentId: Long,
        isCompleted: Boolean = false,
    ) = BandalartCellEntity(id = id, title = title, parentId = parentId, isCompleted = isCompleted)
}
