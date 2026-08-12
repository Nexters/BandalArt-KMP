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

import com.nexters.bandalart.core.database.BandalartDatabase
import com.nexters.bandalart.core.database.entity.BandalartDBEntity
import com.nexters.bandalart.core.database.entity.BandalartWidgetSnapshotDto
import com.nexters.bandalart.core.database.openExistingSharedBandalartDatabase
import kotlinx.coroutines.flow.first
import platform.Foundation.NSUserDefaults

class IosWidgetDataBridge {
    private var sharedDatabase: BandalartDatabase? = null

    private fun database(): BandalartDatabase? = sharedDatabase ?: openExistingSharedBandalartDatabase()?.also { sharedDatabase = it }

    suspend fun getRecentSnapshot(): IosWidgetSnapshot? {
        val dao = database()?.bandalartDao ?: return null
        val defaults = NSUserDefaults(suiteName = IOS_WIDGET_APP_GROUP_IDENTIFIER)
        val bandalartId =
            dao
                .getBandalartList()
                .first()
                .map(BandalartDBEntity::toIosWidgetBandalartRecord)
                .toIosWidgetBandalartOptions()
                .resolveRecentBandalartId(defaults.integerForKey(IOS_WIDGET_RECENT_BANDALART_ID_KEY))
                ?: return null
        val mainCell = dao.findBandalartMainCell(bandalartId)?.cell ?: return null
        val mainCellId = mainCell.id ?: return null
        val subGoalId =
            dao
                .getChildCells(mainCellId)
                .map { cell ->
                    IosWidgetSubGoalRecord(
                        id = cell.id,
                        bandalartId = cell.bandalartId,
                        parentId = cell.parentId,
                        title = cell.title,
                    )
                }.toIosWidgetSubGoalOptions(bandalartId = bandalartId, mainCellId = mainCellId)
                .resolveRecentSubGoalId(defaults.integerForKey(IOS_WIDGET_RECENT_SUB_GOAL_ID_KEY))

        return dao.findWidgetSnapshot(bandalartId, subGoalId)?.toIosWidgetSnapshotRecord()?.toIosWidgetSnapshot()
    }

    suspend fun setTaskCompleted(
        bandalartId: Long,
        subGoalId: Long,
        taskId: Long,
        completed: Boolean,
    ): IosWidgetSnapshot? {
        if (bandalartId <= 0L || subGoalId <= 0L || taskId <= 0L) return null
        val dao = database()?.bandalartDao ?: return null
        val wasUpdated =
            dao.setTaskCompletedIfOwned(
                bandalartId = bandalartId,
                subGoalId = subGoalId,
                taskId = taskId,
                completed = completed,
            )
        if (!wasUpdated) return null
        return dao.findWidgetSnapshot(bandalartId, subGoalId)?.toIosWidgetSnapshotRecord()?.toIosWidgetSnapshot()
    }
}

private const val IOS_WIDGET_APP_GROUP_IDENTIFIER = "group.com.nexters.bandalart"
private const val IOS_WIDGET_RECENT_BANDALART_ID_KEY = "ios_widget_recent_bandalart_id"
private const val IOS_WIDGET_RECENT_SUB_GOAL_ID_KEY = "ios_widget_recent_sub_goal_id"

private fun BandalartDBEntity.toIosWidgetBandalartRecord() =
    IosWidgetBandalartRecord(
        id = id,
        title = title,
        profileEmoji = profileEmoji,
    )

private fun BandalartWidgetSnapshotDto.toIosWidgetSnapshotRecord() =
    IosWidgetSnapshotRecord(
        bandalartId = requireNotNull(bandalart.id),
        subGoalId = subGoal?.id,
        title = bandalart.title.orEmpty(),
        profileEmoji = bandalart.profileEmoji,
        completionRatio = bandalart.completionRatio,
        subGoalTitle = subGoal?.title,
        tasks =
            tasks.map { task ->
                IosWidgetTaskRecord(
                    id = task.id,
                    title = task.title,
                    isCompleted = task.isCompleted,
                )
            },
    )
