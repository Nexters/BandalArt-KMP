/*
 * Copyright 2026 easyhooon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nexters.bandalart.ads

import com.nexters.bandalart.core.common.RewardedAdPurpose
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AndroidRewardedAdPolicyTest {
    @Test
    fun bandalartCreationRecordsTheReward() =
        runTest {
            val recordedRequestIds = mutableListOf<Long>()
            val policy =
                AndroidRewardedAdRewardPolicy { requestId ->
                    recordedRequestIds += requestId
                    true
                }

            assertTrue(policy.complete(42L, RewardedAdPurpose.BANDALART_CREATION))
            assertEquals(listOf(42L), recordedRequestIds)
        }

    @Test
    fun cloudBackupDoesNotRecordABandalartSlotReward() =
        runTest {
            val recordedRequestIds = mutableListOf<Long>()
            val policy =
                AndroidRewardedAdRewardPolicy { requestId ->
                    recordedRequestIds += requestId
                    true
                }

            assertTrue(policy.complete(42L, RewardedAdPurpose.CLOUD_BACKUP))
            assertTrue(recordedRequestIds.isEmpty())
        }

    @Test
    fun duplicateAdUnitIdsArePreloadedOnce() {
        assertEquals(
            setOf("shared", "other"),
            distinctRewardedAdUnitIds("shared", "shared", "other"),
        )
    }

    @Test
    fun cancellingALoadingRequestRejectsLateLoadCallbacks() {
        val lifecycle = RewardedAdRequestLifecycle()

        assertEquals(RewardedAdConsumeAction.CANCEL, lifecycle.consume())

        assertFalse(lifecycle.acceptsPresentationCallbacks)
        assertFalse(lifecycle.tryMarkPendingActivity())
        assertFalse(lifecycle.tryMarkShowing())
        assertEquals(RewardedAdFinishAction.IGNORE, lifecycle.finish())
    }

    @Test
    fun cancellingAnActivityPendingRequestPreventsItFromShowingLater() {
        val lifecycle = RewardedAdRequestLifecycle()
        assertTrue(lifecycle.tryMarkPendingActivity())

        assertEquals(RewardedAdConsumeAction.CANCEL, lifecycle.consume())

        assertFalse(lifecycle.acceptsPresentationCallbacks)
        assertFalse(lifecycle.tryMarkShowing())
        assertEquals(RewardedAdFinishAction.IGNORE, lifecycle.finish())
    }

    @Test
    fun abandoningAnAlreadyShowingRequestCompletesAndRemovesIt() {
        val lifecycle = RewardedAdRequestLifecycle()
        assertTrue(lifecycle.tryMarkShowing())

        assertEquals(RewardedAdConsumeAction.ABANDON_SHOWING, lifecycle.consume())

        assertEquals(RewardedAdFinishAction.COMPLETE_AND_REMOVE, lifecycle.finish())
        assertEquals(RewardedAdFinishAction.IGNORE, lifecycle.finish())
    }
}
