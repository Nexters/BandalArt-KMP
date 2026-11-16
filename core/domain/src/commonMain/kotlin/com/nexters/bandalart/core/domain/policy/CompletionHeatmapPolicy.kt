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
package com.nexters.bandalart.core.domain.policy

import com.nexters.bandalart.core.domain.entity.CompletionHeatmap
import com.nexters.bandalart.core.domain.entity.CompletionHeatmapDay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

const val COMPLETION_HEATMAP_WEEKS = 16

/** 이번 주를 포함한 최근 [weeks]주의 첫날(월요일) */
fun completionHeatmapStartOf(
    today: LocalDate,
    weeks: Int = COMPLETION_HEATMAP_WEEKS,
): LocalDate = weekStartOf(today).minus(DatePeriod(days = (weeks - 1) * 7))

/** 하루 완료 수를 고정 기준으로 0~4 색 단계로 바꾼다. */
fun completionHeatmapLevel(count: Int): Int =
    when {
        count <= 0 -> 0
        count == 1 -> 1
        count <= 3 -> 2
        count <= 6 -> 3
        else -> 4
    }

/** 완료 기록 한 건당 하나씩 담긴 [completedDates]를 최근 [weeks]주의 날짜별 완료 수로 묶는다. */
fun calculateCompletionHeatmap(
    completedDates: Collection<LocalDate>,
    today: LocalDate,
    weeks: Int = COMPLETION_HEATMAP_WEEKS,
): CompletionHeatmap {
    val startDate = completionHeatmapStartOf(today, weeks)
    val countsByDate = completedDates.filter { it in startDate..today }.groupingBy { it }.eachCount()
    val days =
        generateSequence(startDate) { it.plus(DatePeriod(days = 1)) }
            .takeWhile { it <= today }
            .map { date ->
                val count = countsByDate[date] ?: 0
                CompletionHeatmapDay(date = date, count = count, level = completionHeatmapLevel(count))
            }.toList()
    return CompletionHeatmap(
        startDate = startDate,
        endDate = today,
        days = days,
        activeDayCount = countsByDate.size,
        completedCount = countsByDate.values.sum(),
    )
}
