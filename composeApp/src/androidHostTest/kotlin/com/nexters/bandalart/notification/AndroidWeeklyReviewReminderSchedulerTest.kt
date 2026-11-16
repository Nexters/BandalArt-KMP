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
package com.nexters.bandalart.notification

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@ExtendWith(RobolectricExtension::class)
@Config(sdk = [35])
class AndroidWeeklyReviewReminderSchedulerTest {
    private lateinit var application: Application
    private lateinit var workManager: WorkManager
    private val clock = MutableClock(Clock.System.now())

    @BeforeEach
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            application,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        workManager = WorkManager.getInstance(application)
    }

    @Test
    fun scheduleKeepsSingleWorkForSameTargetAndCancelClearsIt() =
        runTest {
            val scheduler = AndroidWeeklyReviewReminderScheduler(application, workManager, clock)

            val result = scheduler.schedule()
            scheduler.schedule()

            assertEquals(1, result.scheduledCount)
            assertNull(result.lastErrorCategory)
            val active = activeWork()
            assertEquals(1, active.size)
            assertTrue(active.single().nextScheduleTimeMillis > clock.now().toEpochMilliseconds())

            scheduler.cancel()

            assertTrue(activeWork().isEmpty())
        }

    @Test
    fun scheduleReplacesPendingWorkForAnotherTarget() =
        runTest {
            val scheduler = AndroidWeeklyReviewReminderScheduler(application, workManager, clock)
            scheduler.schedule()
            val previous = activeWork().single()

            clock.current += 7.days
            scheduler.schedule()

            val active = activeWork()
            assertEquals(1, active.size)
            assertTrue(active.single().id != previous.id)
        }

    private suspend fun activeWork(): List<WorkInfo> =
        workManager
            .getWorkInfosByTagFlow(WeeklyReviewReminderWork.FEATURE_TAG)
            .first()
            .filterNot { info -> info.state == WorkInfo.State.CANCELLED }

    private class MutableClock(
        var current: Instant,
    ) : Clock {
        override fun now(): Instant = current
    }
}
