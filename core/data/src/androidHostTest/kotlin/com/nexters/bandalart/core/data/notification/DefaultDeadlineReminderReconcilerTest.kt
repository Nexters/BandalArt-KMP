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

import com.nexters.bandalart.core.domain.entity.ThemeMode
import com.nexters.bandalart.core.domain.notification.DeadlineNotificationAuthorization
import com.nexters.bandalart.core.domain.notification.DeadlineNotificationAuthorizationStatus
import com.nexters.bandalart.core.domain.notification.DeadlineReminderBatch
import com.nexters.bandalart.core.domain.notification.DeadlineReminderCandidate
import com.nexters.bandalart.core.domain.notification.DeadlineReminderPlanner
import com.nexters.bandalart.core.domain.notification.DeadlineReminderScheduler
import com.nexters.bandalart.core.domain.notification.DeadlineReminderSchedulingErrorCategory
import com.nexters.bandalart.core.domain.notification.DeadlineReminderSchedulingResult
import com.nexters.bandalart.core.domain.notification.DeadlineReminderTimeZoneProvider
import com.nexters.bandalart.core.domain.repository.DeadlineReminderProjectionRepository
import com.nexters.bandalart.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DefaultDeadlineReminderReconcilerTest {
    private val settings = FakeSettingsRepository()
    private val projection =
        object : DeadlineReminderProjectionRepository {
            override suspend fun getCandidates() = listOf(DeadlineReminderCandidate(1, 7, "launch", "2026-08-10T00:00", false))
        }
    private val scheduler = RecordingScheduler()
    private val authorization = FakeAuthorization()
    private val reconciler =
        DefaultDeadlineReminderReconciler(
            settingsRepository = settings,
            projectionRepository = projection,
            planner =
                DeadlineReminderPlanner(
                    clock =
                        object : Clock {
                            override fun now(): Instant = Instant.parse("2026-08-09T00:00:00Z")
                        },
                    timeZoneProvider = DeadlineReminderTimeZoneProvider { TimeZone.UTC },
                ),
            scheduler = scheduler,
            authorization = authorization,
        )

    @Test
    fun disabledPreferenceClearsAllPlatformState() =
        runTest {
            reconciler.reconcileAll()

            assertEquals(1, scheduler.clearCalls)
            assertTrue(scheduler.replacedBatches.isEmpty())
        }

    @Test
    fun enabledAndAuthorizedPreferenceSchedulesDesiredBatches() =
        runTest {
            settings.setDeadlineReminderEnabled(true)
            authorization.status = DeadlineNotificationAuthorizationStatus.GRANTED

            reconciler.reconcileAll()

            assertEquals(listOf("deadline.v1.board.7.date.2026-08-10"), scheduler.replacedBatches.map { it.id })
            assertEquals(1, reconciler.schedulingHealth.value.scheduledCount)
        }

    @Test
    fun blockedAuthorizationClearsPlatformStateWithoutLosingUserPreference() =
        runTest {
            settings.setDeadlineReminderEnabled(true)
            authorization.status = DeadlineNotificationAuthorizationStatus.BLOCKED

            reconciler.reconcileAll()

            assertEquals(1, scheduler.clearCalls)
            assertTrue(settings.deadlineReminderEnabled.value)
            assertEquals(
                DeadlineReminderSchedulingErrorCategory.AUTHORIZATION,
                reconciler.schedulingHealth.value.lastErrorCategory,
            )
        }

    @Test
    fun partialPlatformSchedulingIsIncludedInOverflowHealth() =
        runTest {
            settings.setDeadlineReminderEnabled(true)
            authorization.status = DeadlineNotificationAuthorizationStatus.GRANTED
            scheduler.scheduledCount = 0
            scheduler.lastErrorCategory = DeadlineReminderSchedulingErrorCategory.SCHEDULING

            reconciler.reconcileAll()

            assertEquals(1, reconciler.schedulingHealth.value.overflowCount)
            assertEquals(
                DeadlineReminderSchedulingErrorCategory.SCHEDULING,
                reconciler.schedulingHealth.value.lastErrorCategory,
            )
        }

    private class RecordingScheduler : DeadlineReminderScheduler {
        var clearCalls = 0
        var replacedBatches = emptyList<DeadlineReminderBatch>()
        var scheduledCount: Int? = null
        var lastErrorCategory: DeadlineReminderSchedulingErrorCategory? = null

        override suspend fun replaceAll(batches: List<DeadlineReminderBatch>): DeadlineReminderSchedulingResult {
            replacedBatches = batches
            return DeadlineReminderSchedulingResult(
                scheduledCount = scheduledCount ?: batches.size,
                lastErrorCategory = lastErrorCategory,
            )
        }

        override suspend fun clearAll(): DeadlineReminderSchedulingResult {
            clearCalls += 1
            return DeadlineReminderSchedulingResult(scheduledCount = 0)
        }
    }

    private class FakeAuthorization : DeadlineNotificationAuthorization {
        var status = DeadlineNotificationAuthorizationStatus.REQUESTABLE

        override suspend fun getStatus() = status

        override suspend fun requestAuthorization() = status

        override suspend fun openSettings() = Unit
    }

    private class FakeSettingsRepository : SettingsRepository {
        override val themeMode: Flow<ThemeMode> = MutableStateFlow(ThemeMode.SYSTEM)
        override val recentEmojis: Flow<List<String>> = MutableStateFlow(emptyList())
        override val deadlineReminderEnabled = MutableStateFlow(false)

        override suspend fun setThemeMode(themeMode: ThemeMode) = Unit

        override suspend fun addRecentEmoji(emoji: String) = Unit

        override suspend fun setDeadlineReminderEnabled(enabled: Boolean) {
            deadlineReminderEnabled.value = enabled
        }
    }
}
