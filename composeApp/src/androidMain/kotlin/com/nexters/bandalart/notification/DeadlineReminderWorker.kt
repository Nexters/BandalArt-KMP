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
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nexters.bandalart.core.domain.notification.DeadlineNotificationAuthorizationStatus
import com.nexters.bandalart.core.domain.notification.DeadlineReminderDueDateParser
import com.nexters.bandalart.shared.R
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

interface AndroidDeadlineReminderDependencies {
    val deadlineReminderProjectionRepository:
        com.nexters.bandalart.core.domain.repository.DeadlineReminderProjectionRepository
    val settingsRepository: com.nexters.bandalart.core.domain.repository.SettingsRepository
    val deadlineNotificationAuthorization:
        com.nexters.bandalart.core.domain.notification.DeadlineNotificationAuthorization
    val deadlineReminderReconciler:
        com.nexters.bandalart.core.domain.notification.DeadlineReminderReconciler

    fun createDeadlineNotificationLaunchIntent(
        batchId: String,
        bandalartId: Long,
    ): Intent

    fun captureDeadlineNotification(
        notificationId: Int,
        title: String,
        body: String,
        data: Map<String, String>,
    )
}

object AndroidDeadlineReminderDependenciesRegistry {
    private var dependencies: AndroidDeadlineReminderDependencies? = null

    fun install(dependencies: AndroidDeadlineReminderDependencies) {
        this.dependencies = dependencies
    }

    internal fun get(): AndroidDeadlineReminderDependencies? = dependencies

    internal fun clearForTest() {
        dependencies = null
    }
}

class DeadlineReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val batchId = inputData.getString(DeadlineReminderWork.KEY_BATCH_ID) ?: return Result.success()
        val bandalartId = inputData.getLong(DeadlineReminderWork.KEY_BANDALART_ID, -1L)
        val dueDate = inputData.getString(DeadlineReminderWork.KEY_DUE_DATE)?.let(LocalDate::parse) ?: return Result.success()
        if (bandalartId <= 0L || !DeadlineReminderWork.isFeatureBatchId(batchId)) return Result.success()

        val dependencies = AndroidDeadlineReminderDependenciesRegistry.get() ?: return Result.retry()
        if (!dependencies.settingsRepository.deadlineReminderEnabled.first()) return Result.success()
        if (
            dependencies.deadlineNotificationAuthorization.getStatus() !=
            DeadlineNotificationAuthorizationStatus.GRANTED
        ) {
            return Result.success()
        }

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        if (now.date != dueDate || now.hour < 9) return Result.success()
        val items =
            dependencies.deadlineReminderProjectionRepository
                .getCandidates()
                .asSequence()
                .filter { candidate ->
                    candidate.bandalartId == bandalartId &&
                        !candidate.isCompleted &&
                        !candidate.title.isNullOrBlank() &&
                        DeadlineReminderDueDateParser.parse(candidate.dueDate) == dueDate
                }.map { candidate -> candidate.title.orEmpty().trim() }
                .sorted()
                .toList()
        if (items.isEmpty()) return Result.success()

        publishNotification(
            batchId = batchId,
            bandalartId = bandalartId,
            dueDate = dueDate,
            items = items,
            dependencies = dependencies,
        )
        return Result.success()
    }

    private fun publishNotification(
        batchId: String,
        bandalartId: Long,
        dueDate: LocalDate,
        items: List<String>,
        dependencies: AndroidDeadlineReminderDependencies,
    ) {
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager.ensureDeadlineReminderChannel(applicationContext)
        val launchIntent =
            dependencies
                .createDeadlineNotificationLaunchIntent(batchId, bandalartId)
                .setAction(ACTION_OPEN_DEADLINE)
                .setData(deadlineNotificationDataUri(batchId))
                .putExtra(EXTRA_BANDALART_ID, bandalartId)
        val contentIntent =
            PendingIntent.getActivity(
                applicationContext,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val title = applicationContext.getString(R.string.deadline_reminder_title)
        val content =
            if (items.size == 1) {
                applicationContext.getString(R.string.deadline_reminder_single_body, items.single())
            } else {
                applicationContext.getString(R.string.deadline_reminder_multiple_body, items.size)
            }
        val notification =
            NotificationCompat
                .Builder(applicationContext, DeadlineReminderWork.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()
        notificationManager.notify(batchId, DeadlineReminderWork.NOTIFICATION_ID, notification)
        dependencies.captureDeadlineNotification(
            notificationId = DeadlineReminderWork.NOTIFICATION_ID,
            title = title,
            body = content,
            data =
                mapOf(
                    DeadlineReminderWork.KEY_BATCH_ID to batchId,
                    DeadlineReminderWork.KEY_BANDALART_ID to bandalartId.toString(),
                    DeadlineReminderWork.KEY_DUE_DATE to dueDate.toString(),
                    "item_count" to items.size.toString(),
                    "action" to ACTION_OPEN_DEADLINE,
                    "data_uri" to deadlineNotificationDataUri(batchId).toString(),
                ),
        )
    }

    companion object {
        const val ACTION_OPEN_DEADLINE = "com.nexters.bandalart.action.OPEN_DEADLINE"
        const val EXTRA_BANDALART_ID = "deadline_bandalart_id"
    }
}

class DeadlineReminderReconcileWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val dependencies = AndroidDeadlineReminderDependenciesRegistry.get() ?: return Result.retry()
        dependencies.deadlineReminderReconciler.reconcileAll()
        return Result.success()
    }
}

internal fun deadlineNotificationDataUri(batchId: String): Uri = Uri.parse("bandalart://deadline/$batchId")
