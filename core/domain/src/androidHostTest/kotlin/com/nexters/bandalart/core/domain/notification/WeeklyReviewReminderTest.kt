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

import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WeeklyReviewReminderTest {
    @Test
    fun nextReminderIsComingSundayEvening() {
        // 2026-03-04는 수요일
        assertEquals(
            LocalDateTime(2026, 3, 8, 20, 0),
            nextWeeklyReviewReminderAt(LocalDateTime(2026, 3, 4, 9, 30)),
        )
    }

    @Test
    fun sundayBeforeReminderTimeUsesSameDay() {
        assertEquals(
            LocalDateTime(2026, 3, 8, 20, 0),
            nextWeeklyReviewReminderAt(LocalDateTime(2026, 3, 8, 19, 59)),
        )
    }

    @Test
    fun sundayAtOrAfterReminderTimeMovesToNextWeek() {
        assertEquals(
            LocalDateTime(2026, 3, 15, 20, 0),
            nextWeeklyReviewReminderAt(LocalDateTime(2026, 3, 8, 20, 0)),
        )
        assertEquals(
            LocalDateTime(2026, 3, 15, 20, 0),
            nextWeeklyReviewReminderAt(LocalDateTime(2026, 3, 8, 23, 0)),
        )
    }

    @Test
    fun bufferedLaunchTargetKeepsPendingUntilAcknowledged() {
        val target = BufferedWeeklyReviewLaunchTarget()

        target.record()
        assertTrue(target.pending.value)

        target.acknowledge()
        assertFalse(target.pending.value)
    }
}
