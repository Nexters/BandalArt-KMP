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

import com.nexters.bandalart.core.common.Language
import com.nexters.bandalart.core.common.Locale

expect class LocalDateTime {
    val year: Int
    val monthValue: Int
    val dayOfMonth: Int

    override fun toString(): String

    companion object {
        fun now(): LocalDateTime

        fun parse(date: String): LocalDateTime

        fun parse(
            date: String,
            pattern: String
        ): LocalDateTime
    }
}

fun String.toFormatDate(locale: Locale): String {
    val dateTime = LocalDateTime.parse(this)
    val year = dateTime.year
    val month = dateTime.monthValue
    val day = dateTime.dayOfMonth

    return when (locale.language) {
        Language.ENGLISH -> "~$year, $month/$day"
        Language.KOREAN -> "~${year}년 ${month}월 ${day}일"
        Language.JAPANESE -> "~${year}年 ${month}月 ${day}日"
    }
}

fun String.toStringLocalDateTime(locale: Locale): String {
    val dueDate = LocalDateTime.parse(this, "yyyy-MM-dd'T'HH:mm")
    return when (locale.language) {
        Language.ENGLISH -> "${dueDate.year}, ${dueDate.monthValue}/${dueDate.dayOfMonth}"
        Language.KOREAN -> "${dueDate.year}년 ${dueDate.monthValue}월 ${dueDate.dayOfMonth}일"
        Language.JAPANESE -> "${dueDate.year}年 ${dueDate.monthValue}月 ${dueDate.dayOfMonth}日"
    }
}

fun String.toLocalDateTime(): LocalDateTime = LocalDateTime.parse(this.substring(0, 16), "yyyy-MM-dd'T'HH:mm")

expect fun String.toColor(): Color
