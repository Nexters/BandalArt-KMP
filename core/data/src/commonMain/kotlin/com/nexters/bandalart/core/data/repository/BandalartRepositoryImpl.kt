/*
 * Copyright 2025 easyhooon
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

package com.nexters.bandalart.core.data.repository

import com.nexters.bandalart.core.data.mapper.toDto
import com.nexters.bandalart.core.data.mapper.toEntity
import com.nexters.bandalart.core.database.BandalartDao
import com.nexters.bandalart.core.datastore.BandalartDataStore
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartEmojiEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartMainCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartSubCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartTaskCellEntity
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BandalartRepositoryImpl(
    private val bandalartDataStore: BandalartDataStore,
    private val bandalartDao: BandalartDao,
) : BandalartRepository {
    override suspend fun createBandalart(): BandalartEntity {
        val bandalartId = bandalartDao.createEmptyBandalart()
        return bandalartDao.getBandalart(bandalartId).toEntity()
    }

    override fun getBandalartList(): Flow<List<BandalartEntity>> =
        bandalartDao
            .getBandalartList()
            .map { list -> list.map { it.toEntity() } }

    override suspend fun getBandalart(bandalartId: Long): BandalartEntity = bandalartDao.getBandalart(bandalartId).toEntity()

    override suspend fun deleteBandalart(bandalartId: Long) {
        val mainCell = bandalartDao.getBandalartMainCell(bandalartId).cell
        mainCell.id?.let { cellId ->
            bandalartDao.deleteCellOrReset(cellId)
        }
    }

    override suspend fun getBandalartMainCell(bandalartId: Long): BandalartCellEntity = bandalartDao.getBandalartMainCell(bandalartId).cell.toEntity()

    override suspend fun getChildCells(parentId: Long): List<BandalartCellEntity> {
        val childCells = bandalartDao.getChildCells(parentId)
        return childCells.map { it.toEntity() }
    }

    override suspend fun updateBandalartMainCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartMainCellEntity: UpdateBandalartMainCellEntity,
    ) {
        bandalartDao.updateMainCellWithDto(cellId, updateBandalartMainCellEntity.toDto())
    }

    override suspend fun updateBandalartSubCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartSubCellEntity: UpdateBandalartSubCellEntity,
    ) {
        bandalartDao.updateSubCellWithDto(cellId, updateBandalartSubCellEntity.toDto())
    }

    override suspend fun updateBandalartTaskCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartTaskCellEntity: UpdateBandalartTaskCellEntity,
    ) {
        bandalartDao.updateTaskCellWithDto(cellId, updateBandalartTaskCellEntity.toDto())
    }

    override suspend fun updateBandalartEmoji(
        bandalartId: Long,
        cellId: Long,
        updateBandalartEmojiEntity: UpdateBandalartEmojiEntity,
    ) {
        bandalartDao.updateEmojiWithDto(cellId, updateBandalartEmojiEntity.toDto())
    }

    override suspend fun deleteBandalartCell(cellId: Long) {
        bandalartDao.deleteCellOrReset(cellId)
    }

    override suspend fun setRecentBandalartId(recentBandalartId: Long) {
        bandalartDataStore.setRecentBandalartId(recentBandalartId)
    }

    override suspend fun getRecentBandalartId(): Long = bandalartDataStore.getRecentBandalartId()

    override suspend fun getPrevBandalartList(): List<Pair<Long, Boolean>> = bandalartDataStore.getPrevBandalartList()

    override suspend fun upsertBandalartId(
        bandalartId: Long,
        isCompleted: Boolean
    ) {
        bandalartDataStore.upsertBandalartId(bandalartId, isCompleted)
    }

    override suspend fun checkCompletedBandalartId(bandalartId: Long): Boolean = bandalartDataStore.checkCompletedBandalartId(bandalartId)

    override suspend fun deleteCompletedBandalartId(bandalartId: Long) {
        bandalartDataStore.deleteBandalartId(bandalartId)
    }
}
