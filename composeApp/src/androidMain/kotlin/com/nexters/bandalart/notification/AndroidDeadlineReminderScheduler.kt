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
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import com.nexters.bandalart.core.domain.notification.DeadlineReminderBatch
import com.nexters.bandalart.core.domain.notification.DeadlineReminderScheduler
import com.nexters.bandalart.core.domain.notification.DeadlineReminderSchedulingErrorCategory
import com.nexters.bandalart.core.domain.notification.DeadlineReminderSchedulingResult
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

class AndroidDeadlineReminderScheduler(
    private val application: Application,
    private val configuredWorkManager: WorkManager? = null,
) : DeadlineReminderScheduler {
    private val workManager: WorkManager
        get() = configuredWorkManager ?: WorkManager.getInstance(application)

    override suspend fun replaceAll(batches: List<DeadlineReminderBatch>): DeadlineReminderSchedulingResult {
        clearFeatureState()

        var scheduledCount = 0
        return try {
            batches.forEach { batch ->
                val target =
                    LocalDateTime(batch.dueDate, LocalTime(hour = 9, minute = 0))
                        .toInstant(TimeZone.currentSystemDefault())
                val delay = target - Clock.System.now()
                if (delay.isPositive()) {
                    val request =
                        OneTimeWorkRequestBuilder<DeadlineReminderWorker>()
                            .setInitialDelay(delay.inWholeMilliseconds.milliseconds.toJavaDuration())
                            .setInputData(
                                workDataOf(
                                    DeadlineReminderWork.KEY_BATCH_ID to batch.id,
                                    DeadlineReminderWork.KEY_BANDALART_ID to batch.bandalartId,
                                    DeadlineReminderWork.KEY_DUE_DATE to batch.dueDate.toString(),
                                ),
                            ).addTag(DeadlineReminderWork.FEATURE_TAG)
                            .addTag(batch.id)
                            .build()
                    workManager
                        .enqueueUniqueWork(
                            DeadlineReminderWork.uniqueWorkName(batch.id),
                            ExistingWorkPolicy.REPLACE,
                            request,
                        ).await()
                    scheduledCount += 1
                }
            }
            DeadlineReminderSchedulingResult(scheduledCount = scheduledCount)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            DeadlineReminderSchedulingResult(
                scheduledCount = scheduledCount,
                lastErrorCategory = DeadlineReminderSchedulingErrorCategory.SCHEDULING,
            )
        }
    }

    override suspend fun clearAll(): DeadlineReminderSchedulingResult =
        try {
            clearFeatureState()
            DeadlineReminderSchedulingResult(scheduledCount = 0)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            DeadlineReminderSchedulingResult(
                scheduledCount = 0,
                lastErrorCategory = DeadlineReminderSchedulingErrorCategory.CANCELLATION,
            )
        }

    private suspend fun clearFeatureState() {
        workManager.cancelAllWorkByTag(DeadlineReminderWork.FEATURE_TAG).await()
        val notificationManager = application.getSystemService(NotificationManager::class.java)
        notificationManager.activeNotifications
            .asSequence()
            .mapNotNull { notification -> notification.tag }
            .filter(DeadlineReminderWork::isFeatureBatchId)
            .forEach { tag -> notificationManager.cancel(tag, DeadlineReminderWork.NOTIFICATION_ID) }
    }
}

internal object DeadlineReminderWork {
    const val FEATURE_TAG = "deadline.v1"
    const val RECONCILE_WORK_NAME = "deadline.v1.reconcile"
    const val KEY_BATCH_ID = "batch_id"
    const val KEY_BANDALART_ID = "bandalart_id"
    const val KEY_DUE_DATE = "due_date"
    const val NOTIFICATION_ID = 0
    const val CHANNEL_ID = "deadline_reminder"

    fun uniqueWorkName(batchId: String): String = "deadline.v1.work.$batchId"

    fun isFeatureBatchId(value: String): Boolean = value.startsWith("deadline.v1.board.")
}
