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

import com.nexters.bandalart.core.domain.notification.DeadlineReminderSchedulingErrorCategory
import com.nexters.bandalart.core.domain.notification.DeadlineReminderSchedulingResult
import com.nexters.bandalart.core.domain.notification.WEEKLY_REVIEW_REMINDER_TIME
import com.nexters.bandalart.core.domain.notification.WeeklyReviewReminderScheduler
import platform.Foundation.NSBundle
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

class IosWeeklyReviewReminderScheduler(
    private val notificationCenter: UNUserNotificationCenter = UNUserNotificationCenter.currentNotificationCenter(),
) : WeeklyReviewReminderScheduler {
    /** 같은 식별자로 다시 추가하면 기존 요청을 교체하므로 반복 예약을 여러 번 호출해도 하나만 남는다. */
    override suspend fun schedule(): DeadlineReminderSchedulingResult {
        // 시간대를 지정하지 않은 반복 trigger는 기기의 현재 시간대를 따른다.
        val components =
            NSDateComponents().apply {
                weekday = SUNDAY_WEEKDAY
                hour = WEEKLY_REVIEW_REMINDER_TIME.hour.toLong()
                minute = WEEKLY_REVIEW_REMINDER_TIME.minute.toLong()
            }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = true)
        val content =
            UNMutableNotificationContent().apply {
                setTitle(localized(WEEKLY_REVIEW_REMINDER_TITLE_KEY))
                setBody(localized(WEEKLY_REVIEW_REMINDER_BODY_KEY))
                setSound(UNNotificationSound.defaultSound)
            }
        val request = UNNotificationRequest.requestWithIdentifier(WEEKLY_REVIEW_REMINDER_IDENTIFIER, content, trigger)
        val error =
            awaitUserNotificationCallback { resume ->
                notificationCenter.addNotificationRequest(request, resume)
            }
        return if (error == null) {
            DeadlineReminderSchedulingResult(scheduledCount = 1)
        } else {
            DeadlineReminderSchedulingResult(
                scheduledCount = 0,
                lastErrorCategory = DeadlineReminderSchedulingErrorCategory.SCHEDULING,
            )
        }
    }

    override suspend fun cancel(): DeadlineReminderSchedulingResult {
        notificationCenter.removePendingNotificationRequestsWithIdentifiers(listOf(WEEKLY_REVIEW_REMINDER_IDENTIFIER))
        notificationCenter.removeDeliveredNotificationsWithIdentifiers(listOf(WEEKLY_REVIEW_REMINDER_IDENTIFIER))
        return DeadlineReminderSchedulingResult(scheduledCount = 0)
    }
}

private fun localized(key: String): String = NSBundle.mainBundle.localizedStringForKey(key, value = null, table = null)

private const val SUNDAY_WEEKDAY = 1L
private const val WEEKLY_REVIEW_REMINDER_IDENTIFIER = "weekly_review.v1"
private const val WEEKLY_REVIEW_REMINDER_TITLE_KEY = "weekly_review_reminder_title"
private const val WEEKLY_REVIEW_REMINDER_BODY_KEY = "weekly_review_reminder_body"
