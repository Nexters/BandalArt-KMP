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

import com.nexters.bandalart.core.common.RewardedAdResult
import kotlin.random.Random

internal class RewardedCreateCoordinator {
    private var state = State.IDLE
    private var activeRequestId: Long? = null
    private var expectedCreatedCount: Int? = null

    fun beginSlotCheck(): Boolean {
        if (state != State.IDLE) return false
        state = State.CHECKING_SLOTS
        return true
    }

    fun slotsResolved(
        canCreate: Boolean,
        currentCount: Int,
    ): Boolean {
        check(state == State.CHECKING_SLOTS)
        state = if (canCreate) State.CREATING else State.AWAITING_CONFIRMATION
        if (canCreate) expectedCreatedCount = currentCount + 1
        return canCreate
    }

    fun slotCheckFailed() {
        check(state == State.CHECKING_SLOTS)
        state = State.IDLE
    }

    fun creationFinished(
        wasCreated: Boolean,
        currentCount: Int,
    ) {
        check(state == State.CREATING)
        finishCreation(wasCreated, currentCount)
    }

    fun confirm(): Long? {
        if (state != State.AWAITING_CONFIRMATION) return null
        state = State.SHOWING_AD
        return Random.nextLong().also { requestId -> activeRequestId = requestId }
    }

    fun adFinished(
        requestId: Long,
        result: RewardedAdResult,
    ): RewardedCompletion {
        if (state != State.SHOWING_AD || activeRequestId != requestId) {
            return RewardedCompletion.IGNORED
        }

        activeRequestId = null
        return if (result == RewardedAdResult.DISMISSED) {
            state = State.IDLE
            RewardedCompletion.DISMISSED
        } else {
            state = State.APPLYING_GRANT
            RewardedCompletion.GRANTED
        }
    }

    fun adPreparationFailed(requestId: Long) {
        if (state != State.SHOWING_AD || activeRequestId != requestId) return
        activeRequestId = null
        state = State.IDLE
    }

    fun grantFinished(
        wasCreated: Boolean,
        expectedCount: Int,
        currentCount: Int,
    ) {
        check(state == State.APPLYING_GRANT)
        expectedCreatedCount = expectedCount
        finishCreation(wasCreated, currentCount)
    }

    fun beginPendingRecovery(expectedCount: Int): Boolean {
        if (state != State.IDLE) return false
        expectedCreatedCount = expectedCount
        state = State.APPLYING_GRANT
        return true
    }

    fun creationObserved(currentCount: Int) {
        if (state != State.AWAITING_CREATION) return
        if (currentCount < requireNotNull(expectedCreatedCount)) return
        expectedCreatedCount = null
        state = State.IDLE
    }

    private fun finishCreation(
        wasCreated: Boolean,
        currentCount: Int,
    ) {
        if (!wasCreated || currentCount >= requireNotNull(expectedCreatedCount)) {
            expectedCreatedCount = null
            state = State.IDLE
        } else {
            state = State.AWAITING_CREATION
        }
    }

    fun dismissDialog() {
        if (state == State.AWAITING_CONFIRMATION) state = State.IDLE
    }

    private enum class State {
        IDLE,
        CHECKING_SLOTS,
        CREATING,
        AWAITING_CONFIRMATION,
        SHOWING_AD,
        APPLYING_GRANT,
        AWAITING_CREATION,
    }
}

internal enum class RewardedCompletion {
    IGNORED,
    DISMISSED,
    GRANTED,
}
