package com.nexters.bandalart.core.domain.repository

interface BandalartSlotRepository {
    suspend fun getMaxBandalartSlots(currentBandalartCount: Int): Int

    suspend fun expandMaxBandalartSlots(currentBandalartCount: Int): Int
}
