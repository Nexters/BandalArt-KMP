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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OnboardingRecordSampleTest {
    @Test
    fun sampleMatchesTheCopyShownOnTheCard() {
        // 이슈 #500 예시 카드 문구: '3일 연속', '이번 주 4개 완료'
        assertEquals(3, OnboardingRecordSample.streakDays)
        assertEquals(4, OnboardingRecordSample.thisWeekCompletedCount)
    }

    @Test
    fun sampleCoversSixteenWeeksEndingOnWednesday() {
        val weeks = OnboardingRecordSample.weeks

        assertEquals(COMPLETION_HEATMAP_WEEKS, weeks.size)
        assertTrue(weeks.dropLast(1).all { it.size == 7 })
        assertEquals(3, weeks.last().size)
    }

    @Test
    fun activeDayCountCountsOnlyDaysWithCompletions() {
        val expected = OnboardingRecordSample.weeks.flatten().count { it > 0 }

        assertEquals(expected, OnboardingRecordSample.activeDayCount)
        assertTrue(OnboardingRecordSample.activeDayCount in 1 until COMPLETION_HEATMAP_WEEKS * 7)
    }
}
