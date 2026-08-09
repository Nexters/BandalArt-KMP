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

import com.nexters.bandalart.core.domain.notification.DeadlineReminderBatch
import com.nexters.bandalart.core.domain.notification.DeadlineReminderScheduler
import com.nexters.bandalart.core.domain.notification.DeadlineReminderSchedulingErrorCategory
import com.nexters.bandalart.core.domain.notification.DeadlineReminderSchedulingResult
import kotlinx.datetime.number
import platform.Foundation.NSBundle
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSDateComponents
import platform.Foundation.NSError
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone
import platform.Foundation.timeIntervalSinceNow
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNErrorCodeNotificationInvalidNoContent
import platform.UserNotifications.UNErrorCodeNotificationInvalidNoDate
import platform.UserNotifications.UNErrorCodeNotificationsNotAllowed
import platform.UserNotifications.UNErrorDomain
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

class IosDeadlineReminderScheduler(
    private val notificationCenter: UNUserNotificationCenter = UNUserNotificationCenter.currentNotificationCenter(),
) : DeadlineReminderScheduler {
    override suspend fun replaceAll(batches: List<DeadlineReminderBatch>): DeadlineReminderSchedulingResult {
        clearFeatureNotifications()
        var scheduledCount = 0
        batches.forEach { batch ->
            val request =
                batch.toNotificationRequest()
                    ?: return DeadlineReminderSchedulingResult(
                        scheduledCount = scheduledCount,
                        lastErrorCategory = DeadlineReminderSchedulingErrorCategory.SCHEDULING,
                    )
            val error = notificationCenter.add(request)
            if (error != null) {
                return DeadlineReminderSchedulingResult(
                    scheduledCount = scheduledCount,
                    lastErrorCategory = error.toSchedulingErrorCategory(),
                )
            }
            scheduledCount += 1
        }
        return DeadlineReminderSchedulingResult(scheduledCount = scheduledCount)
    }

    override suspend fun clearAll(): DeadlineReminderSchedulingResult {
        clearFeatureNotifications()
        return DeadlineReminderSchedulingResult(scheduledCount = 0)
    }

    private suspend fun clearFeatureNotifications() {
        val pendingIdentifiers =
            notificationCenter
                .pendingRequests()
                .map { request -> request.identifier() }
                .filter(::isDeadlineReminderIdentifier)
        if (pendingIdentifiers.isNotEmpty()) {
            notificationCenter.removePendingNotificationRequestsWithIdentifiers(pendingIdentifiers)
        }
        val deliveredIdentifiers =
            notificationCenter
                .deliveredNotifications()
                .map { notification -> notification.request.identifier }
                .filter(::isDeadlineReminderIdentifier)
        if (deliveredIdentifiers.isNotEmpty()) {
            notificationCenter.removeDeliveredNotificationsWithIdentifiers(deliveredIdentifiers)
        }
    }
}

private fun DeadlineReminderBatch.toNotificationRequest(): UNNotificationRequest? {
    // NSTimeZone.localTimeZone is Foundation's auto-updating local-time-zone proxy.
    val autoupdatingTimeZone = NSTimeZone.localTimeZone()
    val calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)
    calendar.timeZone = autoupdatingTimeZone
    val components =
        NSDateComponents().apply {
            this.calendar = calendar
            timeZone = autoupdatingTimeZone
            year = dueDate.year.toLong()
            month = dueDate.month.number.toLong()
            day = dueDate.day.toLong()
            hour = DEADLINE_REMINDER_HOUR
            minute = 0
            second = 0
        }
    val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = false)
    val nextTriggerDate = trigger.nextTriggerDate() ?: return null
    if (nextTriggerDate.timeIntervalSinceNow() <= 0.0) return null

    val content =
        UNMutableNotificationContent().apply {
            setTitle(localized(DEADLINE_REMINDER_TITLE_KEY))
            setBody(
                if (items.size == 1) {
                    localized(DEADLINE_REMINDER_SINGLE_BODY_KEY).replace("%@", items.single().title)
                } else {
                    localized(DEADLINE_REMINDER_MULTIPLE_BODY_KEY).replace("%ld", items.size.toString())
                },
            )
            setSound(UNNotificationSound.defaultSound)
            setUserInfo(mapOf(DEADLINE_BANDALART_ID_KEY to bandalartId.toString()))
        }
    return UNNotificationRequest.requestWithIdentifier(id, content, trigger)
}

private suspend fun UNUserNotificationCenter.pendingRequests(): List<UNNotificationRequest> =
    awaitUserNotificationCallback<List<*>?> { resume ->
        getPendingNotificationRequestsWithCompletionHandler(resume)
    }.orEmpty().filterIsInstance<UNNotificationRequest>()

private suspend fun UNUserNotificationCenter.deliveredNotifications(): List<UNNotification> =
    awaitUserNotificationCallback<List<*>?> { resume ->
        getDeliveredNotificationsWithCompletionHandler(resume)
    }.orEmpty().filterIsInstance<UNNotification>()

private suspend fun UNUserNotificationCenter.add(request: UNNotificationRequest): NSError? =
    awaitUserNotificationCallback { resume ->
        addNotificationRequest(request, resume)
    }

private fun NSError.toSchedulingErrorCategory(): DeadlineReminderSchedulingErrorCategory {
    if (domain != UNErrorDomain) return DeadlineReminderSchedulingErrorCategory.UNKNOWN
    return when (code) {
        UNErrorCodeNotificationsNotAllowed -> DeadlineReminderSchedulingErrorCategory.AUTHORIZATION
        UNErrorCodeNotificationInvalidNoDate,
        UNErrorCodeNotificationInvalidNoContent,
        -> DeadlineReminderSchedulingErrorCategory.SCHEDULING
        else -> DeadlineReminderSchedulingErrorCategory.UNKNOWN
    }
}

private fun localized(key: String): String = NSBundle.mainBundle.localizedStringForKey(key, value = null, table = null)

private fun isDeadlineReminderIdentifier(identifier: String): Boolean = identifier.startsWith(DEADLINE_REMINDER_IDENTIFIER_PREFIX)

internal const val DEADLINE_BANDALART_ID_KEY = "deadline_bandalart_id"
private const val DEADLINE_REMINDER_IDENTIFIER_PREFIX = "deadline.v1."
private const val DEADLINE_REMINDER_HOUR = 9L
private const val DEADLINE_REMINDER_TITLE_KEY = "deadline_reminder_title"
private const val DEADLINE_REMINDER_SINGLE_BODY_KEY = "deadline_reminder_single_body"
private const val DEADLINE_REMINDER_MULTIPLE_BODY_KEY = "deadline_reminder_multiple_body"
