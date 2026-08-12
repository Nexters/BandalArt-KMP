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

internal data class IosWidgetBandalartRecord(
    val id: Long?,
    val title: String?,
    val profileEmoji: String?,
)

internal data class IosWidgetSubGoalRecord(
    val id: Long?,
    val bandalartId: Long,
    val parentId: Long?,
    val title: String?,
)

internal data class IosWidgetSnapshotRecord(
    val bandalartId: Long,
    val subGoalId: Long?,
    val title: String,
    val profileEmoji: String?,
    val completionRatio: Int,
    val subGoalTitle: String?,
    val tasks: List<IosWidgetTaskRecord>,
)

internal data class IosWidgetTaskRecord(
    val id: Long?,
    val title: String?,
    val isCompleted: Boolean,
)

internal fun List<IosWidgetBandalartRecord>.toIosWidgetBandalartOptions(): List<IosWidgetBandalartOption> =
    mapNotNull { bandalart ->
        val id = bandalart.id ?: return@mapNotNull null
        val title = bandalart.title?.takeUnless(String::isBlank) ?: return@mapNotNull null
        IosWidgetBandalartOption(id = id, title = title, profileEmoji = bandalart.profileEmoji)
    }

internal fun List<IosWidgetSubGoalRecord>.toIosWidgetSubGoalOptions(
    bandalartId: Long,
    mainCellId: Long,
): List<IosWidgetSubGoalOption> =
    mapNotNull { subGoal ->
        val id = subGoal.id ?: return@mapNotNull null
        val title = subGoal.title?.takeUnless(String::isBlank) ?: return@mapNotNull null
        if (subGoal.bandalartId != bandalartId || subGoal.parentId != mainCellId) return@mapNotNull null
        IosWidgetSubGoalOption(id = id, bandalartId = bandalartId, title = title)
    }

internal fun List<IosWidgetBandalartOption>.resolveRecentBandalartId(recentBandalartId: Long): Long? =
    firstOrNull { it.id == recentBandalartId }?.id ?: firstOrNull()?.id

internal fun List<IosWidgetSubGoalOption>.resolveRecentSubGoalId(recentSubGoalId: Long): Long? =
    firstOrNull { it.id == recentSubGoalId }?.id ?: firstOrNull()?.id

internal fun IosWidgetSnapshotRecord.toIosWidgetSnapshot(): IosWidgetSnapshot =
    IosWidgetSnapshot(
        bandalartId = bandalartId,
        subGoalId = subGoalId,
        title = title,
        profileEmoji = profileEmoji,
        completionRatio = completionRatio,
        subGoalTitle = subGoalTitle,
        tasks =
            tasks.mapNotNull { task ->
                val id = task.id ?: return@mapNotNull null
                val title = task.title?.takeUnless(String::isBlank) ?: return@mapNotNull null
                IosWidgetTask(id = id, title = title, isCompleted = task.isCompleted)
            },
    )
