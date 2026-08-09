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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

@ExtendWith(RobolectricExtension::class)
@Config(sdk = [35])
class DeadlineReminderWorkerTest {
    private lateinit var context: Context

    @BeforeEach
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @AfterEach
    fun tearDown() {
        AndroidDeadlineReminderDependenciesRegistry.clearForTest()
    }

    @Test
    fun reminderWorkerRetriesWhenProcessDependenciesAreNotInstalledYet() =
        runTest {
            AndroidDeadlineReminderDependenciesRegistry.clearForTest()
            val worker =
                TestListenableWorkerBuilder<DeadlineReminderWorker>(context)
                    .setInputData(
                        workDataOf(
                            DeadlineReminderWork.KEY_BATCH_ID to "deadline.v1.board.1.date.2026-08-09",
                            DeadlineReminderWork.KEY_BANDALART_ID to 1L,
                            DeadlineReminderWork.KEY_DUE_DATE to "2026-08-09",
                        ),
                    ).build()

            assertTrue(worker.doWork() is ListenableWorker.Result.Retry)
        }

    @Test
    fun reconcileWorkerRetriesWhenProcessDependenciesAreNotInstalledYet() =
        runTest {
            AndroidDeadlineReminderDependenciesRegistry.clearForTest()
            val worker = TestListenableWorkerBuilder<DeadlineReminderReconcileWorker>(context).build()

            assertTrue(worker.doWork() is ListenableWorker.Result.Retry)
        }
}
