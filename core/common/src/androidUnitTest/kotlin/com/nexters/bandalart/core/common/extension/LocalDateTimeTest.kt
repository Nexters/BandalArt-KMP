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

import com.nexters.bandalart.core.common.Language
import com.nexters.bandalart.core.common.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.format.DateTimeFormatter

@DisplayName("LocalDateTime 테스트")
class LocalDateTimeTest {
    @Nested
    @DisplayName("now 메소드는")
    inner class NowMethod {
        @Test
        @DisplayName("현재 시간을 반환해야 한다")
        fun testNowReturnsCurrentTime() {
            // when
            val localDateTime = LocalDateTime.now()

            // then
            // 정확한 시간을 비교 X -> 년, 월, 일이 현재와 같은지 확인
            val now = java.time.LocalDateTime.now()
            assertEquals(now.year, localDateTime.year)
            assertEquals(now.monthValue, localDateTime.monthValue)
            assertEquals(now.dayOfMonth, localDateTime.dayOfMonth)
        }
    }

    @Nested
    @DisplayName("parse 메소드는")
    inner class ParseMethod {
        @Test
        @DisplayName("기본 형식의 날짜 문자열을 파싱할 수 있어야 한다")
        fun testParseDefaultFormat() {
            // given
            val dateString = "2025-04-15T14:30:00"

            // when
            val localDateTime = LocalDateTime.parse(dateString)

            // then
            assertEquals(2025, localDateTime.year)
            assertEquals(4, localDateTime.monthValue)
            assertEquals(15, localDateTime.dayOfMonth)
        }

        @Test
        @DisplayName("지정된 패턴으로 날짜 문자열을 파싱할 수 있어야 한다")
        fun testParseWithPattern() {
            // given
            val dateString = "2025-04-15 14:30"
            val pattern = "yyyy-MM-dd HH:mm"

            // when
            val localDateTime = LocalDateTime.parse(dateString, pattern)

            // then
            assertEquals(2025, localDateTime.year)
            assertEquals(4, localDateTime.monthValue)
            assertEquals(15, localDateTime.dayOfMonth)
        }
    }

    @Nested
    @DisplayName("toString 메소드는")
    inner class ToStringMethod {
        @Test
        @DisplayName("지정된 형식으로 날짜를 문자열로 변환해야 한다")
        fun testToString() {
            // given
            val javaDateTime = java.time.LocalDateTime.of(2025, 4, 15, 14, 30)
            val localDateTime = LocalDateTime(javaDateTime)

            // when
            val result = localDateTime.toString()

            // then
            val expected = javaDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
            assertEquals(expected, result)
        }
    }

    @Nested
    @DisplayName("속성 접근")
    inner class PropertyAccess {
        @Test
        @DisplayName("year, monthValue, dayOfMonth 속성을 올바르게 반환해야 한다")
        fun testProperties() {
            // given
            val javaDateTime = java.time.LocalDateTime.of(2025, 4, 15, 14, 30)
            val localDateTime = LocalDateTime(javaDateTime)

            // then
            assertEquals(2025, localDateTime.year)
            assertEquals(4, localDateTime.monthValue)
            assertEquals(15, localDateTime.dayOfMonth)
        }
    }

    @Nested
    @DisplayName("toFormatDate 확장 함수는")
    inner class ToFormatDateExtension {
        @Test
        @DisplayName("한국어 환경에서 날짜를 정확히 포맷해야 한다")
        fun testToFormatDateInKoreanLocale() {
            // given
            val dateString = "2025-04-15T00:00:00"
            val koreanLocale =
                object : Locale {
                    override val language = Language.KOREAN
                }

            // when
            val result = dateString.toFormatDate(koreanLocale)

            // then
            assertEquals("~2025년 4월 15일", result)
        }

        @Test
        @DisplayName("영어 환경에서 날짜를 정확히 포맷해야 한다")
        fun testToFormatDateInEnglishLocale() {
            // given
            val dateString = "2025-04-15T00:00:00"
            val englishLocale =
                object : Locale {
                    override val language = Language.ENGLISH
                }

            // when
            val result = dateString.toFormatDate(englishLocale)

            // then
            assertEquals("~2025, 4/15", result)
        }
    }

    @Nested
    @DisplayName("toStringLocalDateTime 확장 함수는")
    inner class ToStringLocalDateTimeExtension {
        @Test
        @DisplayName("각 언어별로 날짜를 올바르게 변환해야 한다")
        fun testToStringLocalDateTimeInDifferentLocales() {
            // given
            val dateString = "2025-04-15T14:30"

            // when & then - Korean
            val koreanLocale =
                object : Locale {
                    override val language = Language.KOREAN
                }
            assertEquals("2025년 4월 15일", dateString.toStringLocalDateTime(koreanLocale))

            // when & then - English
            val englishLocale =
                object : Locale {
                    override val language = Language.ENGLISH
                }
            assertEquals("2025, 4/15", dateString.toStringLocalDateTime(englishLocale))

            // when & then - Japanese
            val japaneseLocale =
                object : Locale {
                    override val language = Language.JAPANESE
                }
            assertEquals("2025年 4月 15日", dateString.toStringLocalDateTime(japaneseLocale))
        }
    }

    @Nested
    @DisplayName("toLocalDateTime 확장 함수는")
    inner class ToLocalDateTimeExtension {
        @Test
        @DisplayName("문자열에서 LocalDateTime 객체를 생성해야 한다")
        fun testToLocalDateTime() {
            // given
            val dateString = "2025-04-15T14:30:00"

            // when
            val result = dateString.toLocalDateTime()

            // then
            assertEquals(2025, result.year)
            assertEquals(4, result.monthValue)
            assertEquals(15, result.dayOfMonth)
        }
    }
}
