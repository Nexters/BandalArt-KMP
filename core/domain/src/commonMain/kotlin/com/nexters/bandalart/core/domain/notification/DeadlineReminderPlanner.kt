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
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

object DeadlineReminderDueDateParser {
    fun parse(value: String?): LocalDate? =
        value?.let { dueDate ->
            runCatching { LocalDateTime.parse(dueDate).date }.getOrNull()
        }
}

fun interface DeadlineReminderTimeZoneProvider {
    fun currentTimeZone(): TimeZone
}

class DeadlineReminderPlanner(
    private val clock: Clock,
    private val timeZoneProvider: DeadlineReminderTimeZoneProvider,
) {
    fun plan(
        candidates: List<DeadlineReminderCandidate>,
        isEnabled: Boolean,
    ): DeadlineReminderPlan {
        if (!isEnabled) return DeadlineReminderPlan(emptyList(), overflowCount = 0)

        val now =
            kotlinx.datetime.Instant
                .fromEpochMilliseconds(clock.now().toEpochMilliseconds())
                .toLocalDateTime(timeZoneProvider.currentTimeZone())
        val batches =
            candidates
                .asSequence()
                .filterNot(DeadlineReminderCandidate::isCompleted)
                .mapNotNull { candidate -> candidate.toNormalizedItem(now) }
                .groupBy { item -> item.bandalartId to item.dueDate }
                .map { (key, items) ->
                    DeadlineReminderBatch(
                        bandalartId = key.first,
                        dueDate = key.second,
                        items =
                            items
                                .sortedBy(NormalizedDeadlineReminderItem::cellId)
                                .map { item ->
                                    DeadlineReminderItem(
                                        cellId = item.cellId,
                                        title = item.title,
                                    )
                                },
                    )
                }.sortedWith(compareBy(DeadlineReminderBatch::dueDate, DeadlineReminderBatch::bandalartId))

        return DeadlineReminderPlan(
            batches = batches.take(MAX_SCHEDULED_DEADLINE_REMINDER_BATCH_COUNT),
            overflowCount = (batches.size - MAX_SCHEDULED_DEADLINE_REMINDER_BATCH_COUNT).coerceAtLeast(0),
        )
    }

    private fun DeadlineReminderCandidate.toNormalizedItem(now: LocalDateTime,): NormalizedDeadlineReminderItem? {
        val normalizedTitle = title?.trim().orEmpty()
        if (normalizedTitle.isEmpty()) return null

        val normalizedDueDate = DeadlineReminderDueDateParser.parse(dueDate) ?: return null
        val targetDateTime = LocalDateTime(normalizedDueDate, LocalTime(hour = 9, minute = 0))
        if (targetDateTime <= now) return null

        return NormalizedDeadlineReminderItem(
            cellId = cellId,
            bandalartId = bandalartId,
            title = normalizedTitle,
            dueDate = normalizedDueDate,
        )
    }

    private data class NormalizedDeadlineReminderItem(
        val cellId: Long,
        val bandalartId: Long,
        val title: String,
        val dueDate: LocalDate,
    )
}
