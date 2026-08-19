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
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.nexters.bandalart.core.domain.notification.DeadlineReminderBatch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

@ExtendWith(RobolectricExtension::class)
@Config(sdk = [35])
class AndroidDeadlineReminderSchedulerTest {
    private lateinit var application: Application
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: AndroidDeadlineReminderScheduler

    @BeforeEach
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            application,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        workManager = WorkManager.getInstance(application)
        scheduler = AndroidDeadlineReminderScheduler(application, workManager)
    }

    @Test
    fun replaceUsesStableUniqueWorkAndClearCancelsIt() =
        runTest {
            val dueDate =
                Clock.System
                    .now()
                    .plus(2, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
            val batch = DeadlineReminderBatch(bandalartId = 3, dueDate = dueDate, items = emptyList())

            scheduler.replaceAll(listOf(batch))
            scheduler.replaceAll(listOf(batch))

            val active =
                workManager
                    .getWorkInfosByTagFlow(DeadlineReminderWork.FEATURE_TAG)
                    .first()
                    .filterNot { info -> info.state == WorkInfo.State.CANCELLED }
            assertEquals(1, active.size)
            assertTrue(active.single().tags.containsAll(setOf(DeadlineReminderWork.FEATURE_TAG, batch.id)))
            assertTrue(active.single().nextScheduleTimeMillis > System.currentTimeMillis())
            assertTrue(deadlineNotificationDataUri(batch.id) != deadlineNotificationDataUri("${batch.id}.other"))

            scheduler.clearAll()

            val afterClear = workManager.getWorkInfosByTagFlow(DeadlineReminderWork.FEATURE_TAG).first()
            assertTrue(afterClear.all { info -> info.state == WorkInfo.State.CANCELLED })
        }

    @Test
    fun testNotificationPostsWithoutReplacingScheduledDeadlineWork() =
        runTest {
            val dueDate =
                Clock.System
                    .now()
                    .plus(2, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
            val batch = DeadlineReminderBatch(bandalartId = 3, dueDate = dueDate, items = emptyList())
            scheduler.replaceAll(listOf(batch))

            val result = scheduler.postTestNotification()

            assertEquals(1, result.scheduledCount)
            assertEquals(null, result.lastErrorCategory)
            val activeWork =
                workManager
                    .getWorkInfosByTagFlow(DeadlineReminderWork.FEATURE_TAG)
                    .first()
                    .filterNot { info -> info.state == WorkInfo.State.CANCELLED }
            assertEquals(1, activeWork.size)
            val notificationManager = application.getSystemService(NotificationManager::class.java)
            assertTrue(
                notificationManager.activeNotifications.any { notification ->
                    notification.tag == DeadlineReminderWork.TEST_NOTIFICATION_TAG
                },
            )

            scheduler.clearAll()

            assertFalse(
                notificationManager.activeNotifications.any { notification ->
                    notification.tag == DeadlineReminderWork.TEST_NOTIFICATION_TAG
                },
            )
        }
}
