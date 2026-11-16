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

import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.core.domain.entity.CellCompletionRecord
import com.nexters.bandalart.core.domain.entity.WeeklyReviewBandalartSummary
import com.nexters.bandalart.core.domain.entity.WeeklyReviewSummary
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

/** [today]가 속한 주의 월요일 */
fun weekStartOf(today: LocalDate): LocalDate = today.minus(DatePeriod(days = today.dayOfWeek.isoDayNumber - 1))

/** 이번 주 월요일부터 [today]까지의 완료 기록을 반다라트별로 묶는다. 삭제된 반다라트의 기록은 합계에만 센다. */
fun calculateWeeklyReviewSummary(
    records: Collection<CellCompletionRecord>,
    bandalarts: List<BandalartEntity>,
    today: LocalDate,
): WeeklyReviewSummary {
    val weekStart = weekStartOf(today)
    val weekRecords = records.filter { it.completedDate in weekStart..today }
    val countsByBandalartId = weekRecords.groupingBy { it.bandalartId }.eachCount()
    return WeeklyReviewSummary(
        weekStart = weekStart,
        weekEnd = today,
        completedCount = weekRecords.size,
        activeDayCount = weekRecords.distinctBy { it.completedDate }.size,
        bandalarts =
            bandalarts
                .mapNotNull { bandalart ->
                    val count = countsByBandalartId[bandalart.id] ?: return@mapNotNull null
                    WeeklyReviewBandalartSummary(
                        bandalartId = bandalart.id,
                        title = bandalart.title,
                        profileEmoji = bandalart.profileEmoji,
                        mainColor = bandalart.mainColor,
                        completedCount = count,
                        completionRatio = bandalart.completionRatio,
                    )
                }.sortedWith(compareByDescending<WeeklyReviewBandalartSummary> { it.completedCount }.thenBy { it.bandalartId }),
    )
}
