package com.nexters.bandalart.core.data.repository

import com.nexters.bandalart.core.datastore.BandalartDataStore
import com.nexters.bandalart.core.domain.policy.resolveMaxBandalartSlots
import com.nexters.bandalart.core.domain.repository.BandalartSlotRepository
import javax.inject.Inject

internal class BandalartSlotRepositoryImpl @Inject constructor(
    private val bandalartDataStore: BandalartDataStore,
) : BandalartSlotRepository {
    override suspend fun getMaxBandalartSlots(currentBandalartCount: Int): Int {
        return bandalartDataStore.resolveMaxBandalartSlots(
            minimumSlots = resolveMaxBandalartSlots(currentBandalartCount),
        )
    }

    override suspend fun expandMaxBandalartSlots(currentBandalartCount: Int): Int {
        return bandalartDataStore.expandMaxBandalartSlots(
            minimumSlots = resolveMaxBandalartSlots(currentBandalartCount),
        )
    }
}
