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

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nexters.bandalart.core.common.DeadlineNotificationPermissionHistory
import com.nexters.bandalart.core.domain.notification.DeadlineNotificationAuthorization
import com.nexters.bandalart.core.domain.notification.DeadlineNotificationAuthorizationStatus

class AndroidDeadlineNotificationAuthorization(
    private val application: Application,
) : DeadlineNotificationAuthorization {
    override suspend fun getStatus(): DeadlineNotificationAuthorizationStatus {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(application, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_DENIED
        ) {
            return if (DeadlineNotificationPermissionHistory.isBlocked(application)) {
                DeadlineNotificationAuthorizationStatus.BLOCKED
            } else {
                DeadlineNotificationAuthorizationStatus.REQUESTABLE
            }
        }
        if (!NotificationManagerCompat.from(application).areNotificationsEnabled()) {
            return DeadlineNotificationAuthorizationStatus.BLOCKED
        }
        val channel =
            application
                .getSystemService(NotificationManager::class.java)
                .getNotificationChannel(DeadlineReminderWork.CHANNEL_ID)
        return if (channel?.importance == NotificationManager.IMPORTANCE_NONE) {
            DeadlineNotificationAuthorizationStatus.BLOCKED
        } else {
            DeadlineNotificationAuthorizationStatus.GRANTED
        }
    }

    override suspend fun requestAuthorization(): DeadlineNotificationAuthorizationStatus = getStatus()

    override suspend fun openSettings() {
        val intent =
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, application.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }
}
