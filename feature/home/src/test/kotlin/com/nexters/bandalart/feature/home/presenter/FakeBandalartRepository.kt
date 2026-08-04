package com.nexters.bandalart.feature.home.presenter

import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartEmojiEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartMainCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartSubCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartTaskCellEntity
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeBandalartRepository(
    bandalarts: List<BandalartEntity>,
    var recentBandalartId: Long = bandalarts.firstOrNull()?.id ?: 0L,
    private val createdBandalart: BandalartEntity? = null,
) : BandalartRepository {
    private val bandalartFlow = MutableStateFlow(bandalarts)
    private val createCalled = CompletableDeferred<Unit>()

    var createCalls = 0
        private set

    suspend fun awaitCreate() = createCalled.await()

    override suspend fun createBandalart(): BandalartEntity? {
        createCalls += 1
        createCalled.complete(Unit)
        return createdBandalart
    }

    override fun getBandalartList(): Flow<List<BandalartEntity>> = bandalartFlow

    override suspend fun getBandalart(bandalartId: Long): BandalartEntity {
        return bandalartFlow.value.find { it.id == bandalartId }
            ?: createdBandalart?.takeIf { it.id == bandalartId }
            ?: error("Unknown bandalart: $bandalartId")
    }

    override suspend fun getBandalartMainCell(bandalartId: Long): BandalartCellEntity {
        return BandalartCellEntity(id = bandalartId, bandalartId = bandalartId, parentId = null)
    }

    override suspend fun getChildCells(parentId: Long): List<BandalartCellEntity> = emptyList()

    override suspend fun setRecentBandalartId(recentBandalartId: Long) {
        this.recentBandalartId = recentBandalartId
    }

    override suspend fun getRecentBandalartId(): Long = recentBandalartId

    override suspend fun getPrevBandalartList(): List<Pair<Long, Boolean>> {
        return bandalartFlow.value.map { it.id to it.isCompleted }
    }

    override suspend fun upsertBandalartId(bandalartId: Long, isCompleted: Boolean) = Unit
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
    override suspend fun checkCompletedBandalartId(bandalartId: Long): Boolean = false
    override suspend fun deleteCompletedBandalartId(bandalartId: Long) = error("Not used")
}
