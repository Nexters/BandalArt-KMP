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

/** 한 주에 고를 수 있는 할 일 칸 수 */
const val WEEKLY_FOCUS_TASK_LIMIT = 3

/**
 * 이번 주 할 일 선택 시트에 보일 할 일 칸.
 *
 * @property subGoalTitle 할 일이 속한 상위 목표 제목
 */
data class WeeklyFocusTask(
    val cell: BandalartCellEntity,
    val subGoalTitle: String?,
)

/** [mainCell] 표에서 제목이 있는 할 일 칸을 상위 목표 순서, 그 안의 칸 순서로 나열한다. */
private fun titledTasksOf(mainCell: BandalartCellEntity): List<WeeklyFocusTask> =
    mainCell.children.flatMap { subCell ->
        subCell.children
            .filterNot { it.title.isNullOrBlank() }
            .map { WeeklyFocusTask(cell = it, subGoalTitle = subCell.title) }
    }

/** 고른 순서대로, 현재 표에 제목이 남아 있는 할 일만 돌려준다. 지운 칸은 제목이 비므로 빠진다. */
fun weeklyFocusTasksOf(
    mainCell: BandalartCellEntity,
    selectedIds: List<Long>,
): List<WeeklyFocusTask> {
    val tasksById = titledTasksOf(mainCell).associateBy { it.cell.id }
    return selectedIds.distinct().mapNotNull(tasksById::get)
}

/** 고를 수 있는 할 일: 미완료 칸과, 완료됐어도 해제할 수 있게 이미 고른 칸 */
fun weeklyFocusCandidatesOf(
    mainCell: BandalartCellEntity,
    selectedIds: Collection<Long>,
): List<WeeklyFocusTask> = titledTasksOf(mainCell).filter { !it.cell.isCompleted || it.cell.id in selectedIds }
