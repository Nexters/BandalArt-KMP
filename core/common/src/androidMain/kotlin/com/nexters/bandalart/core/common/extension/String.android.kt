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
import java.time.format.DateTimeFormatter

actual class LocalDateTime(
    private val datetime: java.time.LocalDateTime
) {
    actual val year: Int get() = datetime.year
    actual val monthValue: Int get() = datetime.monthValue
    actual val dayOfMonth: Int get() = datetime.dayOfMonth

    actual override fun toString(): String = datetime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))

    actual companion object {
        actual fun now(): LocalDateTime = LocalDateTime(java.time.LocalDateTime.now())

        actual fun parse(date: String): LocalDateTime = LocalDateTime(java.time.LocalDateTime.parse(date))

        actual fun parse(
            date: String,
            pattern: String
        ): LocalDateTime {
            val formatter = DateTimeFormatter.ofPattern(pattern)
            return LocalDateTime(java.time.LocalDateTime.parse(date, formatter))
        }
    }
}

actual fun String.toColor(): Color = Color(android.graphics.Color.parseColor(this))
