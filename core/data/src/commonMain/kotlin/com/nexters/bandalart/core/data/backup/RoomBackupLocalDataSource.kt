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

package com.nexters.bandalart.core.data.backup

import com.nexters.bandalart.core.database.BandalartDao
import com.nexters.bandalart.core.database.entity.BandalartCellDBEntity
import com.nexters.bandalart.core.database.entity.BandalartDBEntity
import com.nexters.bandalart.core.datastore.BandalartBackupPreferences
import com.nexters.bandalart.core.datastore.BandalartDataStore
import com.nexters.bandalart.core.domain.backup.BackupBandalart
import com.nexters.bandalart.core.domain.backup.BackupCell
import com.nexters.bandalart.core.domain.backup.BackupCompletedBandalart
import com.nexters.bandalart.core.domain.backup.BackupPreferences
import com.nexters.bandalart.core.domain.backup.BackupSnapshot

class RoomBackupLocalDataSource(
    private val bandalartDao: BandalartDao,
    private val bandalartDataStore: BandalartDataStore,
) : BackupLocalDataSource {
    override suspend fun hasData(): Boolean = bandalartDao.getAllBandalarts().isNotEmpty()

    override suspend fun createSnapshot(): BackupSnapshot {
        val bandalarts = bandalartDao.getAllBandalarts()
        val cells = bandalartDao.getAllCells()
        val preferences =
            bandalartDataStore.createBackupPreferences(
                bandalarts.mapNotNull(BandalartDBEntity::id),
            )
        return BackupSnapshot(
            bandalarts = bandalarts.map(BandalartDBEntity::toBackup),
            cells = cells.map(BandalartCellDBEntity::toBackup),
            preferences = preferences.toBackup(),
        )
    }

    override suspend fun restoreSnapshot(snapshot: BackupSnapshot) {
        val previousIds = bandalartDao.getAllBandalarts().mapNotNull(BandalartDBEntity::id)
        bandalartDao.replaceAllForBackup(
            bandalarts = snapshot.bandalarts.map(BackupBandalart::toDatabase),
            cells = snapshot.cells.map(BackupCell::toDatabase),
        )
        bandalartDataStore.restoreBackupPreferences(
            backup = snapshot.preferences.toDataStore(),
            bandalartIdsToClear = (previousIds + snapshot.bandalarts.map(BackupBandalart::id)).distinct(),
        )
    }
}

private fun BandalartDBEntity.toBackup() =
    BackupBandalart(
        id = checkNotNull(id),
        mainColor = mainColor,
        subColor = subColor,
        profileEmoji = profileEmoji,
        title = title,
        description = description,
        dueDate = dueDate,
        isCompleted = isCompleted,
        completionRatio = completionRatio,
    )

private fun BandalartCellDBEntity.toBackup() =
    BackupCell(
        id = checkNotNull(id),
        bandalartId = bandalartId,
        title = title,
        description = description,
        dueDate = dueDate,
        isCompleted = isCompleted,
        parentId = parentId,
    )

private fun BandalartBackupPreferences.toBackup() =
    BackupPreferences(
        recentBandalartId = recentBandalartId,
        recentSubGoalIds = recentSubGoalIds,
        completedBandalarts = completedBandalarts.map { (id, completed) -> BackupCompletedBandalart(id, completed) },
        onboardingCompleted = onboardingCompleted,
        themeMode = themeMode,
        recentEmojis = recentEmojis,
        deadlineReminderEnabled = deadlineReminderEnabled,
        maxBandalartSlots = maxBandalartSlots,
    )

private fun BackupBandalart.toDatabase() =
    BandalartDBEntity(
        id = id,
        mainColor = mainColor,
        subColor = subColor,
        profileEmoji = profileEmoji,
        title = title,
        description = description,
        dueDate = dueDate,
        isCompleted = isCompleted,
        completionRatio = completionRatio,
    )

private fun BackupCell.toDatabase() =
    BandalartCellDBEntity(
        id = id,
        bandalartId = bandalartId,
        title = title,
        description = description,
        dueDate = dueDate,
        isCompleted = isCompleted,
        parentId = parentId,
    )

private fun BackupPreferences.toDataStore() =
    BandalartBackupPreferences(
        recentBandalartId = recentBandalartId,
        recentSubGoalIds = recentSubGoalIds,
        completedBandalarts = completedBandalarts.map { it.bandalartId to it.isCompleted },
        onboardingCompleted = onboardingCompleted,
        themeMode = themeMode,
        recentEmojis = recentEmojis,
        deadlineReminderEnabled = deadlineReminderEnabled,
        maxBandalartSlots = maxBandalartSlots,
    )
