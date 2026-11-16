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

package com.nexters.bandalart.feature.home.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.weekly_review_heatmap_less
import bandalart.core.designsystem.generated.resources.weekly_review_heatmap_more
import bandalart.core.designsystem.generated.resources.weekly_review_heatmap_summary
import bandalart.core.designsystem.generated.resources.weekly_review_heatmap_title
import bandalart.core.designsystem.generated.resources.weekly_review_period
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily
import com.nexters.bandalart.core.domain.entity.CompletionHeatmap
import com.nexters.bandalart.core.domain.entity.CompletionHeatmapDay
import com.nexters.bandalart.core.domain.policy.COMPLETION_HEATMAP_WEEKS
import org.jetbrains.compose.resources.stringResource

private val MaxCellSize = 16.dp
private val CellGap = 3.dp
private const val DAYS_PER_WEEK = 7

// 1~4단계 색. 0단계는 테마의 surfaceVariant를 쓴다.
private val LightLevelColors = listOf(Color(0xFFC5FFEA), Color(0xFF3FFFBA), Color(0xFF12C98A), Color(0xFF0B8A5E))
private val DarkLevelColors = listOf(Color(0xFF124A39), Color(0xFF16805D), Color(0xFF25C48D), Color(0xFF3FFFBA))

@Composable
internal fun CompletionHeatmapSection(
    heatmap: CompletionHeatmap,
    modifier: Modifier = Modifier,
) {
    val levelColors = heatmapLevelColors()
    // 스크린리더는 112칸을 하나씩 읽지 않고 제목·기간·요약만 한 번에 읽는다.
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .semantics(mergeDescendants = true) {},
    ) {
        Text(
            text = stringResource(Res.string.weekly_review_heatmap_title, COMPLETION_HEATMAP_WEEKS),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontFamily = pretendardFontFamily(),
            fontWeight = FontWeight.W600,
            letterSpacing = (-0.32).sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text =
                stringResource(
                    Res.string.weekly_review_period,
                    heatmap.startDate.monthNumber,
                    heatmap.startDate.dayOfMonth,
                    heatmap.endDate.monthNumber,
                    heatmap.endDate.dayOfMonth,
                ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontFamily = pretendardFontFamily(),
        )
        Text(
            text = stringResource(Res.string.weekly_review_heatmap_summary, heatmap.activeDayCount, heatmap.completedCount),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontFamily = pretendardFontFamily(),
            modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        CompletionHeatmapGrid(
            days = heatmap.days,
            levelColors = levelColors,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Spacer(modifier = Modifier.height(8.dp))
        CompletionHeatmapLegend(
            levelColors = levelColors,
            modifier =
                Modifier
                    .align(Alignment.End)
                    .clearAndSetSemantics {},
        )
    }
}

@Composable
private fun heatmapLevelColors(): List<Color> {
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return listOf(emptyColor) + if (isDark) DarkLevelColors else LightLevelColors
}

@Composable
private fun CompletionHeatmapGrid(
    days: List<CompletionHeatmapDay>,
    levelColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    // 첫날이 월요일이므로 7일씩 끊으면 열 하나가 한 주(위가 월요일)가 된다.
    val weeks = days.chunked(DAYS_PER_WEEK)
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cellSize = min(MaxCellSize, (maxWidth - CellGap * (weeks.size - 1)) / weeks.size)
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(CellGap),
        ) {
            weeks.forEach { week ->
                Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
                    week.forEach { day ->
                        HeatmapCell(color = levelColors[day.level], size = cellSize)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletionHeatmapLegend(
    levelColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(CellGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendLabel(text = stringResource(Res.string.weekly_review_heatmap_less))
        levelColors.forEach { HeatmapCell(color = it, size = 10.dp) }
        LegendLabel(text = stringResource(Res.string.weekly_review_heatmap_more))
    }
}

@Composable
private fun LegendLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        fontFamily = pretendardFontFamily(),
    )
}

@Composable
private fun HeatmapCell(
    color: Color,
    size: Dp,
) {
    Box(
        modifier =
            Modifier
                .size(size)
                .background(color = color, shape = RoundedCornerShape(2.dp)),
    )
}
