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

package com.nexters.bandalart.di.metro

import android.content.Intent
import com.nexters.bandalart.core.domain.notification.DeadlineNotificationAuthorization
import com.nexters.bandalart.core.domain.notification.DeadlineReminderReconciler
import com.nexters.bandalart.core.domain.repository.DeadlineReminderProjectionRepository
import com.nexters.bandalart.core.domain.repository.SettingsRepository
import com.nexters.bandalart.notification.AndroidDeadlineReminderDependencies
import com.nexters.bandalart.notification.AndroidDeadlineReminderDependenciesRegistry
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class AndroidDeadlineReminderInfrastructureTest {
    @AfterEach
    fun tearDown() {
        AndroidDeadlineReminderDependenciesRegistry.clearForTest()
    }

    @Test
    fun installationReconcilesPersistedRemindersAtColdStart() =
        runTest {
            val reconciler = mockk<DeadlineReminderReconciler>(relaxed = true)
            val dependencies =
                object : AndroidDeadlineReminderDependencies {
                    override val deadlineReminderProjectionRepository =
                        mockk<DeadlineReminderProjectionRepository>()
                    override val settingsRepository = mockk<SettingsRepository>()
                    override val deadlineNotificationAuthorization =
                        mockk<DeadlineNotificationAuthorization>()
                    override val deadlineReminderReconciler = reconciler

                    override fun createDeadlineNotificationLaunchIntent(
                        batchId: String,
                        bandalartId: Long,
                    ): Intent = mockk()

                    override fun captureDeadlineNotification(
                        notificationId: Int,
                        title: String,
                        body: String,
                        data: Map<String, String>,
                    ) = Unit
                }

            installAndroidDeadlineReminderInfrastructure(
                dependencies = dependencies,
                startupScope = this,
            ).join()

            coVerify(exactly = 1) { reconciler.reconcileAll() }
        }
}
