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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import com.nexters.bandalart.core.domain.notification.DeadlineReminderSchedulingErrorCategory
import com.nexters.bandalart.core.domain.notification.DeadlineReminderSchedulingResult
import com.nexters.bandalart.core.domain.notification.WeeklyReviewReminderScheduler
import com.nexters.bandalart.core.domain.notification.nextWeeklyReviewReminderAt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

class AndroidWeeklyReviewReminderScheduler(
    private val application: Application,
    private val configuredWorkManager: WorkManager? = null,
    private val clock: Clock = Clock.System,
) : WeeklyReviewReminderScheduler {
    private val workManager: WorkManager
        get() = configuredWorkManager ?: WorkManager.getInstance(application)

    /**
     * 다음 일요일 20:00 작업을 예약한다. 발송 시각마다 작업 이름이 달라서, 발송 중인 Worker가 다음 주를 예약해도
     * 자기 자신(RUNNING)은 취소되지 않는다. 대기 중인 이전 시각 작업(시간대 변경 등)만 취소한다.
     */
    override suspend fun schedule(): DeadlineReminderSchedulingResult =
        try {
            val now = clock.now()
            val timeZone = TimeZone.currentSystemDefault()
            val target = nextWeeklyReviewReminderAt(now.toLocalDateTime(timeZone)).toInstant(timeZone)
            val workName = WeeklyReviewReminderWork.uniqueWorkName(target.toEpochMilliseconds())
            workManager
                .getWorkInfosByTagFlow(WeeklyReviewReminderWork.FEATURE_TAG)
                .first()
                .filter { info -> info.state == WorkInfo.State.ENQUEUED && workName !in info.tags }
                .forEach { info -> workManager.cancelWorkById(info.id).await() }
            val request =
                OneTimeWorkRequestBuilder<WeeklyReviewReminderWorker>()
                    .setInitialDelay((target - now).inWholeMilliseconds.milliseconds.toJavaDuration())
                    .addTag(WeeklyReviewReminderWork.FEATURE_TAG)
                    .addTag(workName)
                    .build()
            workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.KEEP, request).await()
            DeadlineReminderSchedulingResult(scheduledCount = 1)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            DeadlineReminderSchedulingResult(
                scheduledCount = 0,
                lastErrorCategory = DeadlineReminderSchedulingErrorCategory.SCHEDULING,
            )
        }

    override suspend fun cancel(): DeadlineReminderSchedulingResult =
        try {
            workManager.cancelAllWorkByTag(WeeklyReviewReminderWork.FEATURE_TAG).await()
            application
                .getSystemService(NotificationManager::class.java)
                .cancel(WeeklyReviewReminderWork.FEATURE_TAG, WeeklyReviewReminderWork.NOTIFICATION_ID)
            DeadlineReminderSchedulingResult(scheduledCount = 0)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            DeadlineReminderSchedulingResult(
                scheduledCount = 0,
                lastErrorCategory = DeadlineReminderSchedulingErrorCategory.CANCELLATION,
            )
        }
}

internal object WeeklyReviewReminderWork {
    const val FEATURE_TAG = "weekly_review.v1"
    const val NOTIFICATION_ID = 0
    const val CHANNEL_ID = "weekly_review_v1"

    fun uniqueWorkName(targetEpochMillis: Long): String = "weekly_review.v1.work.$targetEpochMillis"
}
