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

package com.nexters.bandalart.core.data.repository

import com.nexters.bandalart.core.datastore.BandalartDataStore
import com.nexters.bandalart.core.domain.policy.resolveMaxBandalartSlots
import com.nexters.bandalart.core.domain.repository.BandalartSlotRepository
import com.nexters.bandalart.core.domain.repository.PendingRewardedCreation

class DefaultBandalartSlotRepository(
    private val bandalartDataStore: BandalartDataStore,
) : BandalartSlotRepository {
    override suspend fun getMaxBandalartSlots(currentBandalartCount: Int): Int =
        bandalartDataStore.resolveMaxBandalartSlots(
            minimumSlots = resolveMaxBandalartSlots(currentBandalartCount),
        )

    override suspend fun expandMaxBandalartSlots(currentBandalartCount: Int): Int =
        bandalartDataStore.expandMaxBandalartSlots(
            minimumSlots = resolveMaxBandalartSlots(currentBandalartCount),
        )

    override suspend fun prepareRewardedCreation(
        requestId: Long,
        currentBandalartCount: Int,
    ): PendingRewardedCreation =
        bandalartDataStore
            .prepareRewardedCreation(
                requestId = requestId,
                minimumSlots = resolveMaxBandalartSlots(currentBandalartCount),
            ).toDomain()

    override suspend fun grantRewardedCreation(requestId: Long): PendingRewardedCreation? =
        bandalartDataStore.grantRewardedCreation(requestId)?.toDomain()

    override suspend fun getPendingRewardedCreation(): PendingRewardedCreation? = bandalartDataStore.getPendingRewardedCreation()?.toDomain()

    override suspend fun clearPendingRewardedCreation(requestId: Long) {
        bandalartDataStore.clearPendingRewardedCreation(requestId)
    }
}

private fun com.nexters.bandalart.core.datastore.StoredPendingRewardedCreation.toDomain() =
    PendingRewardedCreation(
        requestId = requestId,
        targetSlots = targetSlots,
        isGranted = isGranted,
    )
