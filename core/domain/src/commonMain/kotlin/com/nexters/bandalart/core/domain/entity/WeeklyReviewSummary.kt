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
package com.nexters.bandalart.core.domain.entity

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDate

/** 태스크 셀 완료 기록 한 건(셀당 하루 1건) */
data class CellCompletionRecord(
    val bandalartId: Long,
    val completedDate: LocalDate,
)

/**
 * 이번 주(월요일~오늘) 진행률 요약
 *
 * @param completedCount 이번 주 완료 기록 수
 * @param activeDayCount 완료 기록이 있는 날 수
 */
@Stable
data class WeeklyReviewSummary(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val completedCount: Int,
    val activeDayCount: Int,
    val bandalarts: List<WeeklyReviewBandalartSummary>,
)

@Stable
data class WeeklyReviewBandalartSummary(
    val bandalartId: Long,
    val title: String?,
    val profileEmoji: String?,
    val mainColor: String,
    val completedCount: Int,
    val completionRatio: Int,
)
