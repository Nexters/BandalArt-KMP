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

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** 오늘 또는 어제에서 끝나는, 완료 기록이 있는 연속 날짜 수 */
fun calculateCompletionStreak(
    completedDates: Collection<LocalDate>,
    today: LocalDate,
): Int {
    val dates = completedDates.toSet()
    var day =
        when {
            today in dates -> today
            today.minus(ONE_DAY) in dates -> today.minus(ONE_DAY)
            else -> return 0
        }
    var streak = 0
    while (day in dates) {
        streak++
        day = day.minus(ONE_DAY)
    }
    return streak
}

private val ONE_DAY = DatePeriod(days = 1)
