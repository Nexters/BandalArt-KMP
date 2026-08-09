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

import com.nexters.bandalart.core.domain.notification.DeadlineNotificationAuthorization
import com.nexters.bandalart.core.domain.notification.DeadlineNotificationAuthorizationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenNotificationSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNNotificationSettingEnabled
import platform.UserNotifications.UNNotificationSettings
import platform.UserNotifications.UNUserNotificationCenter

class IosDeadlineNotificationAuthorization(
    private val notificationCenter: UNUserNotificationCenter = UNUserNotificationCenter.currentNotificationCenter(),
) : DeadlineNotificationAuthorization {
    override suspend fun getStatus(): DeadlineNotificationAuthorizationStatus = notificationCenter.settings().toStatus()

    override suspend fun requestAuthorization(): DeadlineNotificationAuthorizationStatus {
        awaitUserNotificationCallback<Unit> { resume ->
            notificationCenter.requestAuthorizationWithOptions(
                options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
            ) { _, _ ->
                resume(Unit)
            }
        }
        return getStatus()
    }

    override suspend fun openSettings() {
        withContext(Dispatchers.Main.immediate) {
            val url = NSURL.URLWithString(UIApplicationOpenNotificationSettingsURLString) ?: return@withContext
            UIApplication.sharedApplication.openURL(
                url = url,
                options = emptyMap<Any?, Any?>(),
                completionHandler = null,
            )
        }
    }
}

private suspend fun UNUserNotificationCenter.settings(): UNNotificationSettings =
    checkNotNull(
        awaitUserNotificationCallback<UNNotificationSettings?> { resume ->
            getNotificationSettingsWithCompletionHandler(resume)
        },
    )

private fun UNNotificationSettings.toStatus(): DeadlineNotificationAuthorizationStatus =
    when (authorizationStatus) {
        UNAuthorizationStatusNotDetermined -> DeadlineNotificationAuthorizationStatus.REQUESTABLE
        UNAuthorizationStatusDenied -> DeadlineNotificationAuthorizationStatus.BLOCKED
        UNAuthorizationStatusProvisional,
        UNAuthorizationStatusEphemeral,
        -> DeadlineNotificationAuthorizationStatus.QUIET
        UNAuthorizationStatusAuthorized ->
            if (alertSetting == UNNotificationSettingEnabled) {
                DeadlineNotificationAuthorizationStatus.GRANTED
            } else {
                DeadlineNotificationAuthorizationStatus.QUIET
            }
        else -> DeadlineNotificationAuthorizationStatus.BLOCKED
    }
