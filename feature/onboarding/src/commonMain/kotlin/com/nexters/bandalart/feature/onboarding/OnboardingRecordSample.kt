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

package com.nexters.bandalart.feature.onboarding

import com.nexters.bandalart.core.domain.policy.COMPLETION_HEATMAP_WEEKS

/**
 * 온보딩 기록 소개 장에서 보여 주는 예시 기록. 실제 사용자 기록이 아니다.
 *
 * 열은 주(왼쪽이 과거), 각 문자열은 월요일부터의 하루 완료 수다. 마지막 주는 오늘(수요일)까지만 있다.
 * 연속 일수·이번 주 완료 수·실천한 날 수는 모두 이 배열에서 계산해 카드 문구와 격자가 서로 맞게 한다.
 */
internal object OnboardingRecordSample {
    val weeks: List<List<Int>> =
        listOf(
            "0100010",
            "1000100",
            "0102000",
            "1010010",
            "0120100",
            "1013001",
            "0210110",
            "1201020",
            "2140201",
            "1320150",
            "2142030",
            "1512104",
            "3241270",
            "4153141",
            "2471520",
            "121",
        ).map { week -> week.map { it.digitToInt() } }

    private val days: List<Int> = weeks.flatten()

    val streakDays: Int = days.asReversed().takeWhile { it > 0 }.size

    val thisWeekCompletedCount: Int = weeks.last().sum()

    val activeDayCount: Int = days.count { it > 0 }

    init {
        check(weeks.size == COMPLETION_HEATMAP_WEEKS)
    }
}
