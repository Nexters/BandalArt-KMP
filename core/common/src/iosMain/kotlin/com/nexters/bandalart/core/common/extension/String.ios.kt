/*
 * Copyright 2025 easyhooon
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

package com.nexters.bandalart.core.common.extension

import androidx.compose.ui.graphics.Color
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

actual class LocalDateTime(
    private val datetime: NSDate
) {
    private val calendar = NSCalendar.currentCalendar

    actual val year: Int get() = calendar.component(NSCalendarUnitYear, datetime).toInt()
    actual val monthValue: Int get() = calendar.component(NSCalendarUnitMonth, datetime).toInt()
    actual val dayOfMonth: Int get() = calendar.component(NSCalendarUnitDay, datetime).toInt()

    actual override fun toString(): String {
        val formatter = NSDateFormatter()
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm"
        return formatter.stringFromDate(datetime)
    }

    actual companion object {
        actual fun now(): LocalDateTime = LocalDateTime(NSDate())

        actual fun parse(date: String): LocalDateTime {
            val formatter =
                NSDateFormatter().apply {
                    dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
                }
            return LocalDateTime(formatter.dateFromString(date) ?: NSDate())
        }

        actual fun parse(
            date: String,
            pattern: String
        ): LocalDateTime {
            val formatter =
                NSDateFormatter().apply {
                    dateFormat = pattern
                }
            return LocalDateTime(formatter.dateFromString(date) ?: NSDate())
        }
    }
}

actual fun String.toColor(): Color {
    // iOS에서 hex 문자열을 Color로 변환하는 로직
    val hex = this.replace("#", "")
    val r = hex.substring(0, 2).toInt(16) / 255f
    val g = hex.substring(2, 4).toInt(16) / 255f
    val b = hex.substring(4, 6).toInt(16) / 255f
    return Color(r, g, b)
}
