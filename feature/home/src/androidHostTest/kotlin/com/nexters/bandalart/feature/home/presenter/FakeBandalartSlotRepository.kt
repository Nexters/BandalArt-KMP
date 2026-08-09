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

import com.nexters.bandalart.core.domain.repository.BandalartSlotRepository
import com.nexters.bandalart.core.domain.repository.PendingRewardedCreation
import com.nexters.bandalart.core.domain.template.BandalartTemplateId

class FakeBandalartSlotRepository(
    var maxSlots: Int = Int.MAX_VALUE,
    private val getError: Exception? = null,
    private val expandError: Exception? = null,
    initialPendingRewardedCreation: PendingRewardedCreation? = null,
) : BandalartSlotRepository {
    var getCalls = 0
        private set
    var expandCalls = 0
        private set
    var pendingRewardedCreation: PendingRewardedCreation? = initialPendingRewardedCreation
        private set

    override suspend fun getMaxBandalartSlots(currentBandalartCount: Int): Int {
        getCalls += 1
        getError?.let { throw it }
        return maxSlots
    }

    override suspend fun expandMaxBandalartSlots(currentBandalartCount: Int): Int {
        expandCalls += 1
        expandError?.let { throw it }
        maxSlots = maxOf(maxSlots, currentBandalartCount) + 1
        return maxSlots
    }

    override suspend fun prepareRewardedCreation(
        requestId: Long,
        currentBandalartCount: Int,
        templateId: BandalartTemplateId?,
    ): PendingRewardedCreation =
        PendingRewardedCreation(
            requestId = requestId,
            targetSlots = maxOf(maxSlots, currentBandalartCount) + 1,
            isGranted = false,
            templateId = templateId,
        ).also { pendingRewardedCreation = it }

    override suspend fun grantRewardedCreation(requestId: Long): PendingRewardedCreation? {
        expandCalls += 1
        expandError?.let { throw it }
        val pending = pendingRewardedCreation?.takeIf { it.requestId == requestId } ?: return null
        maxSlots = maxOf(maxSlots, pending.targetSlots)
        return pending.copy(isGranted = true).also { pendingRewardedCreation = it }
    }

    override suspend fun getPendingRewardedCreation(): PendingRewardedCreation? = pendingRewardedCreation

    override suspend fun clearPendingRewardedCreation(requestId: Long) {
        if (pendingRewardedCreation?.requestId == requestId) pendingRewardedCreation = null
    }
}
