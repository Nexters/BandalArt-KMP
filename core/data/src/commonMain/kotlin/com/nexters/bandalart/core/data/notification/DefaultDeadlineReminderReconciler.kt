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
import com.nexters.bandalart.core.domain.notification.DeadlineReminderPlanner
import com.nexters.bandalart.core.domain.notification.DeadlineReminderReconciler
import com.nexters.bandalart.core.domain.notification.DeadlineReminderScheduler
import com.nexters.bandalart.core.domain.notification.DeadlineReminderSchedulingErrorCategory
import com.nexters.bandalart.core.domain.notification.DeadlineReminderSchedulingHealth
import com.nexters.bandalart.core.domain.repository.DeadlineReminderProjectionRepository
import com.nexters.bandalart.core.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultDeadlineReminderReconciler(
    private val settingsRepository: SettingsRepository,
    private val projectionRepository: DeadlineReminderProjectionRepository,
    private val planner: DeadlineReminderPlanner,
    private val scheduler: DeadlineReminderScheduler,
    private val authorization: DeadlineNotificationAuthorization,
) : DeadlineReminderReconciler {
    private val mutex = Mutex()
    private val mutableSchedulingHealth = MutableStateFlow(DeadlineReminderSchedulingHealth())

    override val schedulingHealth: StateFlow<DeadlineReminderSchedulingHealth> = mutableSchedulingHealth.asStateFlow()

    override suspend fun reconcileAll() {
        mutex.withLock {
            try {
                val enabled = settingsRepository.deadlineReminderEnabled.first()
                if (!enabled) {
                    val result = scheduler.clearAll()
                    mutableSchedulingHealth.value =
                        DeadlineReminderSchedulingHealth(
                            scheduledCount = result.scheduledCount,
                            lastErrorCategory = result.lastErrorCategory,
                        )
                    return@withLock
                }

                val authorizationStatus = authorization.getStatus()
                if (
                    authorizationStatus != DeadlineNotificationAuthorizationStatus.GRANTED &&
                    authorizationStatus != DeadlineNotificationAuthorizationStatus.QUIET
                ) {
                    scheduler.clearAll()
                    mutableSchedulingHealth.value =
                        DeadlineReminderSchedulingHealth(
                            lastErrorCategory = DeadlineReminderSchedulingErrorCategory.AUTHORIZATION,
                        )
                    return@withLock
                }

                val plan = planner.plan(projectionRepository.getCandidates(), isEnabled = true)
                val result = scheduler.replaceAll(plan.batches)
                mutableSchedulingHealth.value =
                    DeadlineReminderSchedulingHealth(
                        scheduledCount = result.scheduledCount,
                        overflowCount =
                            plan.overflowCount +
                                (plan.batches.size - result.scheduledCount).coerceAtLeast(0),
                        lastErrorCategory = result.lastErrorCategory,
                    )
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableSchedulingHealth.value =
                    mutableSchedulingHealth.value.copy(
                        lastErrorCategory = DeadlineReminderSchedulingErrorCategory.UNKNOWN,
                    )
            }
        }
    }
}
