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

import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartEmojiEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartMainCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartSubCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartTaskCellEntity
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeBandalartRepository(
    initialBandalarts: List<BandalartEntity>,
    recentBandalartId: Long = 0L,
    private val previousBandalartList: List<Pair<Long, Boolean>> =
        initialBandalarts.map { it.id to it.isCompleted },
    private val createdBandalart: BandalartEntity? = null,
    details: Map<Long, BandalartEntity> = initialBandalarts.associateBy { it.id },
    private val mainCells: Map<Long, BandalartCellEntity> = emptyMap(),
    private val childCells: Map<Long, List<BandalartCellEntity>> = emptyMap(),
) : BandalartRepository {
    private val bandalartFlow = MutableStateFlow(initialBandalarts)
    private val details = details.toMutableMap()

    var recentBandalartId: Long = recentBandalartId
        private set

    var createCalls: Int = 0
        private set

    val completionUpdates = mutableListOf<Pair<Long, Boolean>>()

    override suspend fun createBandalart(): BandalartEntity? {
        createCalls += 1
        return createdBandalart?.also { bandalart ->
            details[bandalart.id] = bandalart
            bandalartFlow.value = bandalartFlow.value + bandalart
        }
    }

    override fun getBandalartList(): Flow<List<BandalartEntity>> = bandalartFlow

    override suspend fun getBandalart(bandalartId: Long): BandalartEntity = requireNotNull(details[bandalartId])

    override suspend fun getBandalartMainCell(bandalartId: Long): BandalartCellEntity? = mainCells[bandalartId]

    override suspend fun getChildCells(parentId: Long): List<BandalartCellEntity> = childCells[parentId].orEmpty()

    override suspend fun setRecentBandalartId(recentBandalartId: Long) {
        this.recentBandalartId = recentBandalartId
    }

    override suspend fun getRecentBandalartId(): Long = recentBandalartId

    override suspend fun getPrevBandalartList(): List<Pair<Long, Boolean>> = previousBandalartList

    override suspend fun upsertBandalartId(
        bandalartId: Long,
        isCompleted: Boolean,
    ) {
        completionUpdates += bandalartId to isCompleted
    }

    override suspend fun deleteBandalart(bandalartId: Long) = error("Not used")

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

    override suspend fun updateBandalartEmoji(
        bandalartId: Long,
        cellId: Long,
        updateBandalartEmojiEntity: UpdateBandalartEmojiEntity,
    ) = error("Not used")

    override suspend fun deleteBandalartCell(cellId: Long) = error("Not used")

    override suspend fun checkCompletedBandalartId(bandalartId: Long): Boolean = error("Not used")

    override suspend fun deleteCompletedBandalartId(bandalartId: Long) = error("Not used")
}
