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
package com.nexters.bandalart.core.domain.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

/** 주간 회고 알림 시각: 매주 일요일 20:00(기기 현지 시각) */
val WEEKLY_REVIEW_REMINDER_DAY_OF_WEEK = DayOfWeek.SUNDAY
val WEEKLY_REVIEW_REMINDER_TIME = LocalTime(hour = 20, minute = 0)

/** [now] 이후(같은 시각 제외) 가장 가까운 주간 회고 알림 시각 */
fun nextWeeklyReviewReminderAt(now: LocalDateTime): LocalDateTime {
    val daysUntil =
        (WEEKLY_REVIEW_REMINDER_DAY_OF_WEEK.isoDayNumber - now.date.dayOfWeek.isoDayNumber + DAYS_PER_WEEK) % DAYS_PER_WEEK
    val candidate = LocalDateTime(now.date.plus(DatePeriod(days = daysUntil)), WEEKLY_REVIEW_REMINDER_TIME)
    return if (candidate > now) candidate else LocalDateTime(candidate.date.plus(DatePeriod(days = DAYS_PER_WEEK)), candidate.time)
}

private const val DAYS_PER_WEEK = 7

interface WeeklyReviewReminderScheduler {
    suspend fun schedule(): DeadlineReminderSchedulingResult

    suspend fun cancel(): DeadlineReminderSchedulingResult
}

object NoOpWeeklyReviewReminderScheduler : WeeklyReviewReminderScheduler {
    override suspend fun schedule(): DeadlineReminderSchedulingResult =
        DeadlineReminderSchedulingResult(
            scheduledCount = 0,
            lastErrorCategory = DeadlineReminderSchedulingErrorCategory.UNSUPPORTED,
        )

    override suspend fun cancel(): DeadlineReminderSchedulingResult = DeadlineReminderSchedulingResult(scheduledCount = 0)
}

interface WeeklyReviewReminderReconciler {
    suspend fun reconcile()
}

object NoOpWeeklyReviewReminderReconciler : WeeklyReviewReminderReconciler {
    override suspend fun reconcile() = Unit
}

interface WeeklyReviewLaunchTarget {
    val pending: StateFlow<Boolean>

    fun record()

    fun acknowledge()
}

class BufferedWeeklyReviewLaunchTarget : WeeklyReviewLaunchTarget {
    private val mutablePending = MutableStateFlow(false)

    override val pending: StateFlow<Boolean> = mutablePending.asStateFlow()

    override fun record() {
        mutablePending.value = true
    }

    override fun acknowledge() {
        mutablePending.value = false
    }
}
