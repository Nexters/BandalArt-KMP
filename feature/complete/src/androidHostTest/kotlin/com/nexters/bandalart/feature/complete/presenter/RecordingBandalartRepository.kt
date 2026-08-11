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

package com.nexters.bandalart.feature.complete.presenter

import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartEmojiEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartMainCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartSubCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartTaskCellEntity
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import com.nexters.bandalart.core.domain.template.BandalartTemplateId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal class RecordingBandalartRepository : BandalartRepository {
    var completion: Pair<Long, Boolean>? = null
        private set

    var completionCount: Int = 0
        private set

    private val completionRecorded = CompletableDeferred<Unit>()

    suspend fun awaitCompletion() = completionRecorded.await()

    override suspend fun upsertBandalartId(
        bandalartId: Long,
        isCompleted: Boolean,
    ) {
        completion = bandalartId to isCompleted
        completionCount += 1
        completionRecorded.complete(Unit)
    }

    override suspend fun createBandalart(templateId: BandalartTemplateId?): BandalartEntity? = error("Not used")

    override fun getBandalartList(): Flow<List<BandalartEntity>> = emptyFlow()

    override suspend fun getBandalart(bandalartId: Long): BandalartEntity = error("Not used")

    override suspend fun deleteBandalart(bandalartId: Long) = error("Not used")

    override suspend fun getBandalartMainCell(bandalartId: Long): BandalartCellEntity? = error("Not used")

    override suspend fun getChildCells(parentId: Long): List<BandalartCellEntity> = error("Not used")

    override suspend fun updateBandalartMainCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartMainCellEntity: UpdateBandalartMainCellEntity,
    ) = error("Not used")

    override suspend fun updateBandalartSubCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartSubCellEntity: UpdateBandalartSubCellEntity,
    ) = error("Not used")

    override suspend fun updateBandalartTaskCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartTaskCellEntity: UpdateBandalartTaskCellEntity,
    ) = error("Not used")

    override suspend fun setTaskCompleted(
        bandalartId: Long,
        subGoalId: Long,
        taskId: Long,
        completed: Boolean,
    ): Boolean = error("Not used")

    override suspend fun updateBandalartEmoji(
        bandalartId: Long,
        cellId: Long,
        updateBandalartEmojiEntity: UpdateBandalartEmojiEntity,
    ) = error("Not used")

    override suspend fun deleteBandalartCell(cellId: Long) = error("Not used")

    override suspend fun setRecentBandalartId(recentBandalartId: Long) = error("Not used")

    override suspend fun getRecentBandalartId(): Long = error("Not used")

    override fun observeRecentBandalartId(): Flow<Long> = emptyFlow()

    override suspend fun getPrevBandalartList(): List<Pair<Long, Boolean>> = error("Not used")

    override suspend fun checkCompletedBandalartId(bandalartId: Long): Boolean = error("Not used")

    override suspend fun deleteCompletedBandalartId(bandalartId: Long) = error("Not used")
}
