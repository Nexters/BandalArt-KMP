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

import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.core.domain.entity.CellCompletionRecord
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WeeklyReviewSummaryPolicyTest {
    // 2026-03-04는 수요일
    private val today = LocalDate(2026, 3, 4)

    @Test
    fun weekStartsOnMonday() {
        assertEquals(LocalDate(2026, 3, 2), weekStartOf(today))
        assertEquals(LocalDate(2026, 3, 2), weekStartOf(LocalDate(2026, 3, 2)))
        assertEquals(LocalDate(2026, 3, 2), weekStartOf(LocalDate(2026, 3, 8)))
    }

    @Test
    fun onlyRecordsFromMondayToTodayAreCounted() {
        val records =
            listOf(
                CellCompletionRecord(bandalartId = 1, completedDate = LocalDate(2026, 3, 1)),
                CellCompletionRecord(bandalartId = 1, completedDate = LocalDate(2026, 3, 2)),
                CellCompletionRecord(bandalartId = 1, completedDate = LocalDate(2026, 3, 4)),
                CellCompletionRecord(bandalartId = 1, completedDate = LocalDate(2026, 3, 4)),
                CellCompletionRecord(bandalartId = 1, completedDate = LocalDate(2026, 3, 5)),
            )

        val summary = calculateWeeklyReviewSummary(records, listOf(bandalart(id = 1)), today)

        assertEquals(LocalDate(2026, 3, 2), summary.weekStart)
        assertEquals(today, summary.weekEnd)
        assertEquals(3, summary.completedCount)
        assertEquals(2, summary.activeDayCount)
        assertEquals(3, summary.bandalarts.single().completedCount)
    }

    @Test
    fun bandalartsAreSortedByCountAndDeletedOnesOnlyCountInTotal() {
        val records =
            listOf(
                CellCompletionRecord(bandalartId = 1, completedDate = today),
                CellCompletionRecord(bandalartId = 2, completedDate = today),
                CellCompletionRecord(bandalartId = 2, completedDate = today),
                CellCompletionRecord(bandalartId = 9, completedDate = today),
            )

        val summary =
            calculateWeeklyReviewSummary(
                records = records,
                bandalarts = listOf(bandalart(id = 1), bandalart(id = 2), bandalart(id = 3)),
                today = today,
            )

        assertEquals(4, summary.completedCount)
        assertEquals(listOf(2L, 1L), summary.bandalarts.map { it.bandalartId })
    }

    @Test
    fun noRecordsMeansEmptySummary() {
        val summary = calculateWeeklyReviewSummary(emptyList(), listOf(bandalart(id = 1)), today)

        assertEquals(0, summary.completedCount)
        assertEquals(0, summary.activeDayCount)
        assertEquals(emptyList<Any>(), summary.bandalarts)
    }

    private fun bandalart(id: Long) =
        BandalartEntity(
            id = id,
            mainColor = "#FF3FFFBA",
            subColor = "#FF111827",
            profileEmoji = null,
            title = "목표 $id",
            description = null,
            dueDate = null,
            isCompleted = false,
            completionRatio = 50,
        )
}
