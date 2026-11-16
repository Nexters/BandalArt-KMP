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
package com.nexters.bandalart.core.data.notification

import com.nexters.bandalart.core.domain.notification.DeadlineNotificationAuthorization
import com.nexters.bandalart.core.domain.notification.DeadlineNotificationAuthorizationStatus
import com.nexters.bandalart.core.domain.notification.DeadlineReminderSchedulingResult
import com.nexters.bandalart.core.domain.notification.WeeklyReviewReminderScheduler
import com.nexters.bandalart.core.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class DefaultWeeklyReviewReminderReconcilerTest {
    private val settings = mockk<SettingsRepository>()
    private val scheduler = mockk<WeeklyReviewReminderScheduler>()
    private val authorization = mockk<DeadlineNotificationAuthorization>()
    private val reconciler = DefaultWeeklyReviewReminderReconciler(settings, scheduler, authorization)

    init {
        coEvery { scheduler.schedule() } returns DeadlineReminderSchedulingResult(scheduledCount = 1)
        coEvery { scheduler.cancel() } returns DeadlineReminderSchedulingResult(scheduledCount = 0)
    }

    @Test
    fun disabledSettingCancelsWithoutCheckingAuthorization() =
        runTest {
            every { settings.weeklyReviewReminderEnabled } returns flowOf(false)

            reconciler.reconcile()

            coVerify(exactly = 1) { scheduler.cancel() }
            coVerify(exactly = 0) { scheduler.schedule() }
            coVerify(exactly = 0) { authorization.getStatus() }
        }

    @ParameterizedTest
    @EnumSource(names = ["GRANTED", "QUIET"])
    fun enabledAndDeliverableSchedules(status: DeadlineNotificationAuthorizationStatus) =
        runTest {
            every { settings.weeklyReviewReminderEnabled } returns flowOf(true)
            coEvery { authorization.getStatus() } returns status

            reconciler.reconcile()

            coVerify(exactly = 1) { scheduler.schedule() }
            coVerify(exactly = 0) { scheduler.cancel() }
        }

    @ParameterizedTest
    @EnumSource(names = ["BLOCKED", "REQUESTABLE", "UNSUPPORTED"])
    fun enabledButNotDeliverableCancels(status: DeadlineNotificationAuthorizationStatus) =
        runTest {
            every { settings.weeklyReviewReminderEnabled } returns flowOf(true)
            coEvery { authorization.getStatus() } returns status

            reconciler.reconcile()

            coVerify(exactly = 1) { scheduler.cancel() }
            coVerify(exactly = 0) { scheduler.schedule() }
        }

    @Test
    fun schedulerFailureIsSwallowed() =
        runTest {
            every { settings.weeklyReviewReminderEnabled } returns flowOf(true)
            coEvery { authorization.getStatus() } returns DeadlineNotificationAuthorizationStatus.GRANTED
            coEvery { scheduler.schedule() } throws IllegalStateException("boom")

            reconciler.reconcile()
        }
}
