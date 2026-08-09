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

import kotlinx.datetime.LocalDate

const val MAX_SCHEDULED_DEADLINE_REMINDER_BATCH_COUNT = 32

data class DeadlineReminderCandidate(
    val cellId: Long,
    val bandalartId: Long,
    val title: String?,
    val dueDate: String?,
    val isCompleted: Boolean,
)

data class DeadlineReminderItem(
    val cellId: Long,
    val title: String,
)

data class DeadlineReminderBatch(
    val bandalartId: Long,
    val dueDate: LocalDate,
    val items: List<DeadlineReminderItem>,
) {
    val id: String = "deadline.v1.board.$bandalartId.date.$dueDate"
}

data class DeadlineReminderPlan(
    val batches: List<DeadlineReminderBatch>,
    val overflowCount: Int,
)

enum class DeadlineReminderSchedulingErrorCategory {
    UNSUPPORTED,
    AUTHORIZATION,
    SCHEDULING,
    CANCELLATION,
    UNKNOWN,
}

data class DeadlineReminderSchedulingResult(
    val scheduledCount: Int,
    val lastErrorCategory: DeadlineReminderSchedulingErrorCategory? = null,
)

data class DeadlineReminderSchedulingHealth(
    val scheduledCount: Int = 0,
    val overflowCount: Int = 0,
    val lastErrorCategory: DeadlineReminderSchedulingErrorCategory? = null,
)

enum class DeadlineNotificationAuthorizationStatus {
    UNSUPPORTED,
    REQUESTABLE,
    GRANTED,
    QUIET,
    BLOCKED,
}
