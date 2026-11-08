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

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nexters.bandalart.core.domain.notification.DeadlineNotificationAuthorizationStatus
import com.nexters.bandalart.core.domain.notification.WEEKLY_REVIEW_REMINDER_DAY_OF_WEEK
import com.nexters.bandalart.core.domain.notification.WEEKLY_REVIEW_REMINDER_TIME
import com.nexters.bandalart.shared.R
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class WeeklyReviewReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val dependencies = AndroidDeadlineReminderDependenciesRegistry.get() ?: return Result.retry()
        if (!dependencies.settingsRepository.weeklyReviewReminderEnabled.first()) return Result.success()
        if (
            dependencies.deadlineNotificationAuthorization.getStatus() !=
            DeadlineNotificationAuthorizationStatus.GRANTED
        ) {
            return Result.success()
        }

        // 기기가 꺼져 있다가 월요일 이후에 늦게 실행되면 새 주의 요약이 비어 보이므로 보내지 않는다.
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        if (now.date.dayOfWeek == WEEKLY_REVIEW_REMINDER_DAY_OF_WEEK && now.time >= WEEKLY_REVIEW_REMINDER_TIME) {
            publishNotification(dependencies)
        }
        dependencies.weeklyReviewReminderReconciler.reconcile()
        return Result.success()
    }

    private fun publishNotification(dependencies: AndroidDeadlineReminderDependencies) {
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager.ensureWeeklyReviewReminderChannel(applicationContext)
        val contentIntent =
            PendingIntent.getActivity(
                applicationContext,
                0,
                dependencies.createWeeklyReviewLaunchIntent().setAction(ACTION_OPEN_WEEKLY_REVIEW),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val content = applicationContext.getString(R.string.weekly_review_reminder_body)
        val notification =
            NotificationCompat
                .Builder(applicationContext, WeeklyReviewReminderWork.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(applicationContext.getString(R.string.weekly_review_reminder_title))
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()
        notificationManager.notify(WeeklyReviewReminderWork.FEATURE_TAG, WeeklyReviewReminderWork.NOTIFICATION_ID, notification)
    }

    companion object {
        const val ACTION_OPEN_WEEKLY_REVIEW = "com.nexters.bandalart.action.OPEN_WEEKLY_REVIEW"
    }
}
