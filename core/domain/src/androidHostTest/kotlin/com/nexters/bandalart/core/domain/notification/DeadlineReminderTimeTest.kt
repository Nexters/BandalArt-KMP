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

import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeadlineReminderTimeTest {
    @Test
    fun minuteOfDayRoundTripsThroughLocalTime() {
        assertEquals(LocalTime(hour = 0, minute = 0), DeadlineReminderTime.fromMinuteOfDay(0))
        assertEquals(LocalTime(hour = 23, minute = 59), DeadlineReminderTime.fromMinuteOfDay(1439))
        assertEquals(450, DeadlineReminderTime.toMinuteOfDay(LocalTime(hour = 7, minute = 30)))
    }

    @Test
    fun outOfRangeStoredValueFallsBackToNineAm() {
        assertEquals(LocalTime(hour = 9, minute = 0), DeadlineReminderTime.fromMinuteOfDay(-1))
        assertEquals(LocalTime(hour = 9, minute = 0), DeadlineReminderTime.fromMinuteOfDay(1440))
        assertEquals(LocalTime(hour = 9, minute = 0), DeadlineReminderTime.Default)
    }
}
