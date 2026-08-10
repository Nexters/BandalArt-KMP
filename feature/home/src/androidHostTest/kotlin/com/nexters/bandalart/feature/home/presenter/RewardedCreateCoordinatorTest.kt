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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RewardedCreateCoordinatorTest {
    @Test
    fun duplicateAndStaleCallbacksAreIgnored() {
        val coordinator = RewardedCreateCoordinator()
        assertTrue(coordinator.beginSlotCheck())
        assertFalse(coordinator.beginSlotCheck())
        assertFalse(coordinator.slotsResolved(canCreate = false, currentCount = 3))

        val requestId = requireNotNull(coordinator.confirm())
        assertNull(coordinator.confirm())
        assertEquals(
            RewardedCompletion.IGNORED,
            coordinator.adFinished(requestId + 1, RewardedAdResult.REWARDED),
        )
        assertEquals(
            RewardedCompletion.GRANTED,
            coordinator.adFinished(requestId, RewardedAdResult.REWARDED),
        )
        assertEquals(
            RewardedCompletion.IGNORED,
            coordinator.adFinished(requestId, RewardedAdResult.REWARDED),
        )
    }

    @Test
    fun dismissedRequestReturnsToIdleWithANewRequestId() {
        val coordinator = RewardedCreateCoordinator()
        val firstRequestId = coordinator.openRewardedRequest()

        assertEquals(
            RewardedCompletion.DISMISSED,
            coordinator.adFinished(firstRequestId, RewardedAdResult.DISMISSED),
        )

        val secondRequestId = coordinator.openRewardedRequest()
        assertNotEquals(firstRequestId, secondRequestId)
    }

    @Test
    fun dialogDismissAndSlotFailureReturnToIdle() {
        val coordinator = RewardedCreateCoordinator()
        assertTrue(coordinator.beginSlotCheck())
        assertFalse(coordinator.slotsResolved(canCreate = false, currentCount = 3))
        coordinator.dismissDialog()
        assertTrue(coordinator.beginSlotCheck())
        coordinator.slotCheckFailed()
        assertTrue(coordinator.beginSlotCheck())
    }

    @Test
    fun creationBlocksNewRequestsUntilTheExpectedCountIsObserved() {
        val coordinator = RewardedCreateCoordinator()
        assertTrue(coordinator.beginSlotCheck())
        assertTrue(coordinator.slotsResolved(canCreate = true, currentCount = 3))
        coordinator.creationFinished(wasCreated = true, currentCount = 3)

        assertFalse(coordinator.beginSlotCheck())
        coordinator.creationObserved(currentCount = 4)
        assertTrue(coordinator.beginSlotCheck())
    }

    @Test
    fun creationObservedBeforeRepositoryReturnsStillUnlocksNextRequest() {
        val coordinator = RewardedCreateCoordinator()
        assertTrue(coordinator.beginSlotCheck())
        assertTrue(coordinator.slotsResolved(canCreate = true, currentCount = 3))

        coordinator.creationObserved(currentCount = 4)
        coordinator.creationFinished(wasCreated = true, currentCount = 3)

        assertTrue(coordinator.beginSlotCheck())
    }

    private fun RewardedCreateCoordinator.openRewardedRequest(): Long {
        assertTrue(beginSlotCheck())
        assertFalse(slotsResolved(canCreate = false, currentCount = 3))
        return requireNotNull(confirm())
    }
}
