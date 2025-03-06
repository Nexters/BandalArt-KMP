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

    override fun getBandalartList(): Flow<List<BandalartEntity>> {
        return bandalartDao.getBandalartList()
            .map { list -> list.map { it.toEntity() } }
    }

    override suspend fun getBandalart(bandalartId: Long): BandalartEntity {
        return bandalartDao.getBandalart(bandalartId).toEntity()
    }

    override suspend fun deleteBandalart(bandalartId: Long) {
        val mainCell = bandalartDao.getBandalartMainCell(bandalartId).cell
        mainCell.id?.let { cellId ->
            bandalartDao.deleteCellOrReset(cellId)
        }
    }

    override suspend fun getBandalartMainCell(bandalartId: Long): BandalartCellEntity {
        return bandalartDao.getBandalartMainCell(bandalartId).cell.toEntity()
    }

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

    override suspend fun getRecentBandalartId(): Long {
        return bandalartDataStore.getRecentBandalartId()
    }

    override suspend fun getPrevBandalartList(): List<Pair<Long, Boolean>> {
        return bandalartDataStore.getPrevBandalartList()
    }

    override suspend fun upsertBandalartId(bandalartId: Long, isCompleted: Boolean) {
        bandalartDataStore.upsertBandalartId(bandalartId, isCompleted)
    }

    override suspend fun checkCompletedBandalartId(bandalartId: Long): Boolean {
        return bandalartDataStore.checkCompletedBandalartId(bandalartId)
    }

    override suspend fun deleteCompletedBandalartId(bandalartId: Long) {
        bandalartDataStore.deleteBandalartId(bandalartId)
    }
}
