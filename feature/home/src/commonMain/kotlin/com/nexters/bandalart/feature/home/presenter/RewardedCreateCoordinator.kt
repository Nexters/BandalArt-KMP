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
    private var latestObservedCount: Int? = null

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
        expectedCreatedCount = if (canCreate) currentCount + 1 else null
        latestObservedCount = if (canCreate) currentCount else null
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
            latestObservedCount = null
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
        latestObservedCount = null
        state = State.APPLYING_GRANT
        return true
    }

    fun creationObserved(currentCount: Int) {
        if (state != State.CREATING && state != State.APPLYING_GRANT && state != State.AWAITING_CREATION) return
        latestObservedCount = maxOf(latestObservedCount ?: currentCount, currentCount)
        if (state != State.AWAITING_CREATION) return
        if (requireNotNull(latestObservedCount) < requireNotNull(expectedCreatedCount)) return
        resetCreation()
    }

    private fun finishCreation(
        wasCreated: Boolean,
        currentCount: Int,
    ) {
        val observedCount = maxOf(latestObservedCount ?: currentCount, currentCount)
        if (!wasCreated || observedCount >= requireNotNull(expectedCreatedCount)) {
            resetCreation()
        } else {
            state = State.AWAITING_CREATION
        }
    }

    private fun resetCreation() {
        expectedCreatedCount = null
        latestObservedCount = null
        state = State.IDLE
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
