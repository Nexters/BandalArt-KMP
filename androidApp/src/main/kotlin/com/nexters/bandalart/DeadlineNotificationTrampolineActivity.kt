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

package com.nexters.bandalart

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.nexters.bandalart.di.metro.recordAndroidDeadlineNotificationLaunch
import com.nexters.bandalart.notification.DeadlineReminderWorker

class DeadlineNotificationTrampolineActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bandalartId =
            intent
                .takeIf { it.action == DeadlineReminderWorker.ACTION_OPEN_DEADLINE }
                ?.getLongExtra(DeadlineReminderWorker.EXTRA_BANDALART_ID, -1L)
                ?: -1L
        if (bandalartId > 0L) {
            recordAndroidDeadlineNotificationLaunch(
                appGraph = (application as BandalartApplication).appGraph,
                bandalartId = bandalartId,
            )
        }
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
        )
        finish()
    }
}
