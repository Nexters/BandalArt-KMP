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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.nexters.bandalart.shared.R

internal fun NotificationManager.ensureDeadlineReminderChannel(context: Context) {
    createNotificationChannel(
        NotificationChannel(
            DeadlineReminderWork.CHANNEL_ID,
            context.getString(R.string.deadline_reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.deadline_reminder_channel_description)
        },
    )
}
