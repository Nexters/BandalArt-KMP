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
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Instant

class DeadlineReminderPlannerTest {
    @Test
    fun dueDateParserNormalizesStoredAndLegacyDateTimesOnly() {
        assertEquals(
            LocalDate(2026, 8, 31),
            DeadlineReminderDueDateParser.parse("2026-08-31T00:00"),
        )
        assertEquals(
            LocalDate(2026, 8, 31),
            DeadlineReminderDueDateParser.parse("2026-08-31T00:00:00"),
        )
        assertNull(DeadlineReminderDueDateParser.parse("2026-08-31"))
        assertNull(DeadlineReminderDueDateParser.parse("invalid"))
        assertNull(DeadlineReminderDueDateParser.parse(null))
    }

    @Test
    fun plannerKeepsOnlyEligibleFutureItemsAndGroupsByBoardAndDate() {
        val planner = plannerAt("2026-08-09T00:00:00Z")

        val plan =
            planner.plan(
                candidates =
                    listOf(
                        candidate(1, 10, " 첫 번째 목표 ", "2026-08-09T00:00"),
                        candidate(2, 10, "두 번째 목표", "2026-08-09T12:30:00"),
                        candidate(3, 10, "완료 목표", "2026-08-09T00:00", isCompleted = true),
                        candidate(4, 10, "   ", "2026-08-09T00:00"),
                        candidate(5, 10, "지난 목표", "2026-08-08T00:00"),
                        candidate(6, 10, "잘못된 날짜", "invalid"),
                    ),
                isEnabled = true,
            )

        assertEquals(1, plan.batches.size)
        assertEquals(
            listOf(
                DeadlineReminderItem(cellId = 1, title = "첫 번째 목표"),
                DeadlineReminderItem(cellId = 2, title = "두 번째 목표"),
            ),
            plan.batches.single().items,
        )
        assertEquals("deadline.v1.board.10.date.2026-08-09", plan.batches.single().id)
        assertEquals(0, plan.overflowCount)
    }

    @Test
    fun plannerUsesTheCurrentTimeZoneOnEveryReconcile() {
        var currentTimeZone: TimeZone = TimeZone.UTC
        val planner =
            DeadlineReminderPlanner(
                clock = FixedClock(Instant.parse("2026-08-09T00:30:00Z")),
                timeZoneProvider = DeadlineReminderTimeZoneProvider { currentTimeZone },
            )
        val candidate = candidate(1, 1, "목표", "2026-08-09T00:00")

        assertEquals(1, planner.plan(listOf(candidate), isEnabled = true).batches.size)

        currentTimeZone = TimeZone.of("Asia/Seoul")

        assertTrue(planner.plan(listOf(candidate), isEnabled = true).batches.isEmpty())
    }

    @Test
    fun plannerSelectsTheNearestThirtyTwoBatchesGlobally() {
        val planner = plannerAt("2026-08-09T00:00:00Z")
        val candidates =
            (33L downTo 1L).map { bandalartId ->
                candidate(
                    cellId = bandalartId,
                    bandalartId = bandalartId,
                    title = "목표 $bandalartId",
                    dueDate = "2026-08-10T00:00",
                )
            }

        val plan = planner.plan(candidates, isEnabled = true)

        assertEquals((1L..32L).toList(), plan.batches.map { it.bandalartId })
        assertEquals(1, plan.overflowCount)

        val refilledPlan =
            planner.plan(
                candidates = candidates.filterNot { it.bandalartId == 1L },
                isEnabled = true,
            )

        assertEquals((2L..33L).toList(), refilledPlan.batches.map { it.bandalartId })
        assertEquals(0, refilledPlan.overflowCount)
    }

    @Test
    fun nearerCrossBoardBatchDisplacesTheFarthestSelection() {
        val planner = plannerAt("2026-08-09T00:00:00Z")
        val initiallySelected =
            (1L..32L).map { bandalartId ->
                candidate(
                    cellId = bandalartId,
                    bandalartId = bandalartId,
                    title = "먼 목표 $bandalartId",
                    dueDate = "2026-09-30T00:00",
                )
            }

        val plan =
            planner.plan(
                candidates =
                    initiallySelected +
                        candidate(
                            cellId = 99,
                            bandalartId = 99,
                            title = "가까운 목표",
                            dueDate = "2026-08-10T00:00",
                        ),
                isEnabled = true,
            )

        assertTrue(plan.batches.any { it.bandalartId == 99L })
        assertTrue(plan.batches.none { it.bandalartId == 32L })
        assertEquals(1, plan.overflowCount)
    }

    @Test
    fun disabledPreferenceProducesNoDesiredBatchesOrOverflow() {
        val planner = plannerAt("2026-08-09T00:00:00Z")

        val plan =
            planner.plan(
                candidates = listOf(candidate(1, 1, "목표", "2026-08-10T00:00")),
                isEnabled = false,
            )

        assertTrue(plan.batches.isEmpty())
        assertEquals(0, plan.overflowCount)
    }

    private fun plannerAt(instant: String): DeadlineReminderPlanner =
        DeadlineReminderPlanner(
            clock = FixedClock(Instant.parse(instant)),
            timeZoneProvider = DeadlineReminderTimeZoneProvider { TimeZone.UTC },
        )

    private fun candidate(
        cellId: Long,
        bandalartId: Long,
        title: String?,
        dueDate: String?,
        isCompleted: Boolean = false,
    ) = DeadlineReminderCandidate(
        cellId = cellId,
        bandalartId = bandalartId,
        title = title,
        dueDate = dueDate,
        isCompleted = isCompleted,
    )

    private class FixedClock(
        private val instant: Instant,
    ) : Clock {
        override fun now(): Instant = instant
    }
}
