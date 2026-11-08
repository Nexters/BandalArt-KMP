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

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CompletionStreakPolicyTest {
    private val today = LocalDate(2026, 3, 1)

    @Test
    fun noRecordsMeansZero() {
        assertEquals(0, calculateCompletionStreak(emptyList(), today))
    }

    @Test
    fun streakEndingTodayCountsConsecutiveDaysAcrossMonthBoundary() {
        val dates = listOf(LocalDate(2026, 3, 1), LocalDate(2026, 2, 28), LocalDate(2026, 2, 27))

        assertEquals(3, calculateCompletionStreak(dates, today))
    }

    @Test
    fun streakEndingYesterdayIsKeptUntilTodayEnds() {
        val dates = listOf(LocalDate(2026, 2, 28), LocalDate(2026, 2, 27))

        assertEquals(2, calculateCompletionStreak(dates, today))
    }

    @Test
    fun missingYesterdayAndTodayBreaksStreak() {
        val dates = listOf(LocalDate(2026, 2, 27), LocalDate(2026, 2, 26))

        assertEquals(0, calculateCompletionStreak(dates, today))
    }

    @Test
    fun gapStopsCounting() {
        val dates = listOf(LocalDate(2026, 3, 1), LocalDate(2026, 2, 27), LocalDate(2026, 2, 26))

        assertEquals(1, calculateCompletionStreak(dates, today))
    }
}
