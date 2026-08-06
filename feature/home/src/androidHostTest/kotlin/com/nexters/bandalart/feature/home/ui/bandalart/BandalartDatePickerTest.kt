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

package com.nexters.bandalart.feature.home.ui.bandalart

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class BandalartDatePickerTest {
    @ParameterizedTest
    @CsvSource(
        "2024, 2, 29, 29",
        "2025, 2, 31, 28",
        "2026, 4, 31, 30",
        "2026, 12, 31, 31",
    )
    fun selectedDateIsAdjustedToTheLastValidDay(
        year: String,
        month: String,
        day: String,
        expectedDay: Int,
    ) {
        val selectedDate = selectedDateWithValidate(year, month, day)

        assertEquals(year.toInt(), selectedDate.year)
        assertEquals(month.toInt(), selectedDate.monthValue)
        assertEquals(expectedDay, selectedDate.dayOfMonth)
    }
}
