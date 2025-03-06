package com.nexters.bandalart.core.common.extension

import androidx.compose.ui.graphics.Color
import java.time.format.DateTimeFormatter

actual class LocalDateTime(private val datetime: java.time.LocalDateTime) {
    actual val year: Int get() = datetime.year
    actual val monthValue: Int get() = datetime.monthValue
    actual val dayOfMonth: Int get() = datetime.dayOfMonth

    actual override fun toString(): String {
        return datetime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
    }

    actual companion object {
        actual fun now(): LocalDateTime {
            return LocalDateTime(java.time.LocalDateTime.now())
        }

        actual fun parse(date: String): LocalDateTime {
            return LocalDateTime(java.time.LocalDateTime.parse(date))
        }

        actual fun parse(date: String, pattern: String): LocalDateTime {
            val formatter = DateTimeFormatter.ofPattern(pattern)
            return LocalDateTime(java.time.LocalDateTime.parse(date, formatter))
        }
    }
}

actual fun String.toColor(): Color {
    return Color(android.graphics.Color.parseColor(this))
}
