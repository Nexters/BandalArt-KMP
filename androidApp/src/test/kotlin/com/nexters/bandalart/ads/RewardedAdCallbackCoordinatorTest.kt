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

package com.nexters.bandalart.ads

import com.nexters.bandalart.core.common.RewardedAdResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RewardedAdCallbackCoordinatorTest {
    @Test
    fun rewardBeforeDismissCompletesAsRewarded() {
        val fixture = Fixture()

        fixture.coordinator.onRewardEarned()
        fixture.coordinator.onDismissed()

        assertEquals(listOf(RewardedAdResult.REWARDED), fixture.results)
        assertTrue(fixture.scheduled.isEmpty())
    }

    @Test
    fun mediationDismissBeforeRewardCompletesAsRewarded() {
        val fixture = Fixture()

        fixture.coordinator.onDismissed()
        fixture.coordinator.onRewardEarned()
        fixture.runScheduled()

        assertEquals(listOf(RewardedAdResult.REWARDED), fixture.results)
    }

    @Test
    fun dismissWaitsWhileRewardIsBeingPersisted() {
        val fixture = Fixture()

        fixture.coordinator.onRewardRecordingStarted()
        fixture.coordinator.onDismissed()
        fixture.runNextScheduled()
        assertTrue(fixture.results.isEmpty())
        fixture.coordinator.onRewardEarned()
        fixture.runScheduled()

        assertEquals(listOf(RewardedAdResult.REWARDED), fixture.results)
    }

    @Test
    fun dismissWithoutRewardCompletesAfterGracePeriod() {
        val fixture = Fixture()

        fixture.coordinator.onDismissed()
        assertTrue(fixture.results.isEmpty())
        fixture.runScheduled()

        assertEquals(listOf(RewardedAdResult.DISMISSED), fixture.results)
    }

    @Test
    fun failureAndLateCallbacksCompleteOnlyOnce() {
        val fixture = Fixture()

        fixture.coordinator.onFailed()
        fixture.coordinator.onDismissed()
        fixture.coordinator.onRewardEarned()
        fixture.runScheduled()

        assertEquals(listOf(RewardedAdResult.FAILED), fixture.results)
    }

    private class Fixture {
        val results = mutableListOf<RewardedAdResult>()
        val scheduled = mutableListOf<() -> Unit>()
        val coordinator =
            RewardedAdCallbackCoordinator(
                finish = results::add,
                scheduleDismissed = scheduled::add,
            )

        fun runScheduled() {
            scheduled.toList().forEach { action -> action() }
        }

        fun runNextScheduled() {
            scheduled.removeFirst()()
        }
    }
}
