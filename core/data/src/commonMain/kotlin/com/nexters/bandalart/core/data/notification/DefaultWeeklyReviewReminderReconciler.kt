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
import com.nexters.bandalart.core.domain.notification.WeeklyReviewReminderReconciler
import com.nexters.bandalart.core.domain.notification.WeeklyReviewReminderScheduler
import com.nexters.bandalart.core.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultWeeklyReviewReminderReconciler(
    private val settingsRepository: SettingsRepository,
    private val scheduler: WeeklyReviewReminderScheduler,
    private val authorization: DeadlineNotificationAuthorization,
) : WeeklyReviewReminderReconciler {
    private val mutex = Mutex()

    override suspend fun reconcile() {
        mutex.withLock {
            try {
                val enabled = settingsRepository.weeklyReviewReminderEnabled.first()
                val authorizationStatus = if (enabled) authorization.getStatus() else null
                if (
                    authorizationStatus == DeadlineNotificationAuthorizationStatus.GRANTED ||
                    authorizationStatus == DeadlineNotificationAuthorizationStatus.QUIET
                ) {
                    scheduler.schedule()
                } else {
                    scheduler.cancel()
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // 다음 재조정 시점(앱 실행, 설정 변경, 시간 변경)에 다시 시도한다.
            }
        }
    }
}
