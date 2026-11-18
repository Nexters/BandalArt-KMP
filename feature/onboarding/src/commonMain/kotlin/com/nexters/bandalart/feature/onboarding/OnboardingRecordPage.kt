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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.onboarding_record_heatmap_summary
import bandalart.core.designsystem.generated.resources.onboarding_record_heatmap_title
import bandalart.core.designsystem.generated.resources.onboarding_record_message
import bandalart.core.designsystem.generated.resources.onboarding_record_sample_description
import bandalart.core.designsystem.generated.resources.onboarding_record_sample_label
import bandalart.core.designsystem.generated.resources.onboarding_record_streak
import bandalart.core.designsystem.generated.resources.onboarding_record_title
import bandalart.core.designsystem.generated.resources.onboarding_record_weekly
import bandalart.core.designsystem.generated.resources.weekly_review_heatmap_less
import bandalart.core.designsystem.generated.resources.weekly_review_heatmap_more
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily
import com.nexters.bandalart.core.domain.policy.COMPLETION_HEATMAP_WEEKS
import com.nexters.bandalart.core.domain.policy.completionHeatmapLevel
import org.jetbrains.compose.resources.stringResource

private val MaxCellSize = 14.dp
private val CellGap = 3.dp
private const val DAYS_PER_WEEK = 7

// 주간 회고 시트의 달성 기록(잔디)과 같은 민트 램프. docs/features/home/COMPLETION_HEATMAP_STRATEGY.md
private val LightLevelColors = listOf(Color(0xFFC5FFEA), Color(0xFF3FFFBA), Color(0xFF12C98A), Color(0xFF0B8A5E))
private val DarkLevelColors = listOf(Color(0xFF124A39), Color(0xFF16805D), Color(0xFF25C48D), Color(0xFF3FFFBA))

/** 목표 만들기 안내 뒤에 보여 주는 달성 기록 소개 장. 본문이 길면 스크롤하고 시작 버튼은 아래에 둔다. */
@Composable
internal fun OnboardingRecordPage(
    bottomContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(50.dp))
            Text(
                text = stringResource(Res.string.onboarding_record_title),
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = pretendardFontFamily(),
                fontWeight = FontWeight.W700,
                fontSize = 22.sp,
                lineHeight = 30.8.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.onboarding_record_message),
                modifier = Modifier.padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = pretendardFontFamily(),
                fontWeight = FontWeight.W500,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))
            OnboardingRecordSampleCard(modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(modifier = Modifier.height(24.dp))
        }
        bottomContent()
    }
}

@Composable
private fun OnboardingRecordSampleCard(modifier: Modifier = Modifier) {
    val levelColors = sampleLevelColors()
    val sample = OnboardingRecordSample
    val description =
        stringResource(
            Res.string.onboarding_record_sample_description,
            sample.streakDays,
            sample.thisWeekCompletedCount,
            COMPLETION_HEATMAP_WEEKS,
            sample.activeDayCount,
        )

    // 스크린리더는 예시 기록이라는 안내와 수치만 한 번에 읽고, 격자 칸은 읽지 않는다.
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
                .clearAndSetSemantics { contentDescription = description },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SampleChip(
                text = stringResource(Res.string.onboarding_record_streak, sample.streakDays),
                containerColor = levelColors[1],
            )
            SampleChip(
                text = stringResource(Res.string.onboarding_record_weekly, sample.thisWeekCompletedCount),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(Res.string.onboarding_record_heatmap_title, COMPLETION_HEATMAP_WEEKS),
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = pretendardFontFamily(),
            fontWeight = FontWeight.W600,
            fontSize = 15.sp,
        )
        Text(
            text = stringResource(Res.string.onboarding_record_heatmap_summary, sample.activeDayCount),
            modifier = Modifier.padding(top = 2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = pretendardFontFamily(),
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        SampleHeatmapGrid(levelColors = levelColors)
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.onboarding_record_sample_label),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = pretendardFontFamily(),
                fontSize = 12.sp,
            )
            SampleLegend(levelColors = levelColors)
        }
    }
}

@Composable
private fun SampleChip(
    text: String,
    containerColor: Color,
) {
    Text(
        text = text,
        modifier =
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(containerColor)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onSurface,
        fontFamily = pretendardFontFamily(),
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
    )
}

@Composable
private fun SampleHeatmapGrid(levelColors: List<Color>) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val weeks = OnboardingRecordSample.weeks
        val cellSize = min(MaxCellSize, (maxWidth - CellGap * (weeks.size - 1)) / weeks.size)
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(CellGap),
        ) {
            weeks.forEach { week ->
                Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
                    repeat(DAYS_PER_WEEK) { dayIndex ->
                        val count = week.getOrNull(dayIndex)
                        if (count == null) {
                            Spacer(modifier = Modifier.size(cellSize))
                        } else {
                            SampleCell(color = levelColors[completionHeatmapLevel(count)], size = cellSize)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SampleLegend(levelColors: List<Color>) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CellGap),
    ) {
        LegendLabel(stringResource(Res.string.weekly_review_heatmap_less))
        Spacer(modifier = Modifier.width(1.dp))
        levelColors.forEach { color -> SampleCell(color = color, size = 10.dp) }
        Spacer(modifier = Modifier.width(1.dp))
        LegendLabel(stringResource(Res.string.weekly_review_heatmap_more))
    }
}

@Composable
private fun LegendLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = pretendardFontFamily(),
        fontSize = 11.sp,
    )
}

@Composable
private fun SampleCell(
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

@Composable
private fun sampleLevelColors(): List<Color> {
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return listOf(emptyColor) + if (isDark) DarkLevelColors else LightLevelColors
}

@Preview
@Composable
private fun OnboardingRecordPagePreview() {
    BandalartTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            OnboardingRecordPage(bottomContent = {})
        }
    }
}
