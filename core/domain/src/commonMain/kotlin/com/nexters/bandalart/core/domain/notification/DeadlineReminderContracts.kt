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

package com.nexters.bandalart.core.domain.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface DeadlineReminderScheduler {
    suspend fun replaceAll(batches: List<DeadlineReminderBatch>): DeadlineReminderSchedulingResult

    suspend fun clearAll(): DeadlineReminderSchedulingResult
}

object NoOpDeadlineReminderScheduler : DeadlineReminderScheduler {
    override suspend fun replaceAll(batches: List<DeadlineReminderBatch>): DeadlineReminderSchedulingResult =
        DeadlineReminderSchedulingResult(
            scheduledCount = 0,
            lastErrorCategory = DeadlineReminderSchedulingErrorCategory.UNSUPPORTED,
        )

    override suspend fun clearAll(): DeadlineReminderSchedulingResult = DeadlineReminderSchedulingResult(scheduledCount = 0)
}

interface DeadlineNotificationAuthorization {
    suspend fun getStatus(): DeadlineNotificationAuthorizationStatus

    suspend fun requestAuthorization(): DeadlineNotificationAuthorizationStatus

    suspend fun openSettings()
}

object NoOpDeadlineNotificationAuthorization : DeadlineNotificationAuthorization {
    override suspend fun getStatus(): DeadlineNotificationAuthorizationStatus = DeadlineNotificationAuthorizationStatus.UNSUPPORTED

    override suspend fun requestAuthorization(): DeadlineNotificationAuthorizationStatus = DeadlineNotificationAuthorizationStatus.UNSUPPORTED

    override suspend fun openSettings() = Unit
}

interface DeadlineReminderReconciler {
    val schedulingHealth: StateFlow<DeadlineReminderSchedulingHealth>

    suspend fun reconcileAll()
}

object NoOpDeadlineReminderReconciler : DeadlineReminderReconciler {
    private val health =
        MutableStateFlow(
            DeadlineReminderSchedulingHealth(
                lastErrorCategory = DeadlineReminderSchedulingErrorCategory.UNSUPPORTED,
            ),
        )

    override val schedulingHealth: StateFlow<DeadlineReminderSchedulingHealth> = health.asStateFlow()

    override suspend fun reconcileAll() = Unit
}
