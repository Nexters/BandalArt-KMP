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

object DeadlineReminderTime {
    const val DEFAULT_MINUTE_OF_DAY = 9 * 60
    private const val MINUTES_PER_DAY = 24 * 60

    val Default: LocalTime = fromMinuteOfDay(DEFAULT_MINUTE_OF_DAY)

    fun fromMinuteOfDay(minuteOfDay: Int): LocalTime {
        val normalized = if (minuteOfDay in 0 until MINUTES_PER_DAY) minuteOfDay else DEFAULT_MINUTE_OF_DAY
        return LocalTime(hour = normalized / 60, minute = normalized % 60)
    }

    fun toMinuteOfDay(time: LocalTime): Int = time.hour * 60 + time.minute
}
