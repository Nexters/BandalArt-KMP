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

/**
 * 최근 몇 주 동안의 날짜별 완료 수(잔디)
 *
 * @param days [startDate]부터 [endDate]까지 빈 날 없이 날짜순
 * @param activeDayCount 완료 기록이 있는 날 수
 * @param completedCount 기간 안의 완료 기록 수
 */
@Stable
data class CompletionHeatmap(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val days: List<CompletionHeatmapDay>,
    val activeDayCount: Int,
    val completedCount: Int,
)

/** @param level 0(기록 없음)부터 4까지의 색 단계 */
data class CompletionHeatmapDay(
    val date: LocalDate,
    val count: Int,
    val level: Int,
)
