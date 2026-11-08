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

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CompletionHeatmapPolicyTest {
    // 2026-11-04는 수요일
    private val wednesday = LocalDate(2026, 11, 4)

    @Test
    fun levelUsesFixedThresholds() {
        val levels = listOf(0, 1, 2, 3, 4, 6, 7, 48).map(::completionHeatmapLevel)

        assertEquals(listOf(0, 1, 2, 2, 3, 3, 4, 4), levels)
    }

    @Test
    fun periodStartsOnMondayFifteenWeeksBeforeThisWeekAndEndsToday() {
        val heatmap = calculateCompletionHeatmap(emptyList(), wednesday)

        assertEquals(LocalDate(2026, 7, 20), heatmap.startDate)
        assertEquals(DayOfWeek.MONDAY, heatmap.startDate.dayOfWeek)
        assertEquals(wednesday, heatmap.endDate)
        assertEquals(15 * 7 + 3, heatmap.days.size)
    }

    @Test
    fun mondayTodayAddsSingleDayColumn() {
        val monday = LocalDate(2026, 11, 9)

        val heatmap = calculateCompletionHeatmap(emptyList(), monday)

        assertEquals(LocalDate(2026, 7, 27), heatmap.startDate)
        assertEquals(15 * 7 + 1, heatmap.days.size)
    }

    @Test
    fun sundayTodayFillsSixteenFullWeeks() {
        val sunday = LocalDate(2026, 11, 8)

        val heatmap = calculateCompletionHeatmap(emptyList(), sunday)

        assertEquals(16 * 7, heatmap.days.size)
    }

    @Test
    fun daysAreContinuousAndEmptyDaysAreZero() {
        val heatmap = calculateCompletionHeatmap(emptyList(), wednesday)

        heatmap.days.zipWithNext().forEach { (prev, next) ->
            assertEquals(prev.date.toEpochDays() + 1, next.date.toEpochDays())
        }
        assertEquals(0, heatmap.days.sumOf { it.count })
        assertEquals(0, heatmap.activeDayCount)
        assertEquals(0, heatmap.completedCount)
    }

    @Test
    fun recordsOnSameDayAreSummedAndOutsidePeriodIsIgnored() {
        val dates =
            listOf(
                wednesday,
                wednesday,
                wednesday,
                LocalDate(2026, 11, 2),
                // 기간 시작일 하루 전과 오늘 다음 날은 제외
                LocalDate(2026, 7, 19),
                LocalDate(2026, 11, 5),
                LocalDate(2026, 7, 20),
            )

        val heatmap = calculateCompletionHeatmap(dates, wednesday)

        val byDate = heatmap.days.associateBy { it.date }
        assertEquals(3, byDate.getValue(wednesday).count)
        assertEquals(2, byDate.getValue(wednesday).level)
        assertEquals(1, byDate.getValue(LocalDate(2026, 11, 2)).count)
        assertEquals(1, byDate.getValue(LocalDate(2026, 7, 20)).count)
        assertEquals(3, heatmap.activeDayCount)
        assertEquals(5, heatmap.completedCount)
    }
}
