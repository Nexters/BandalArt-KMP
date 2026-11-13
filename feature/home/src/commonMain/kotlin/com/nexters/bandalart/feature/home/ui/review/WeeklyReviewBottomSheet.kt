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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.clear_description
import bandalart.core.designsystem.generated.resources.weekly_review_bandalart_progress
import bandalart.core.designsystem.generated.resources.weekly_review_empty
import bandalart.core.designsystem.generated.resources.weekly_review_period
import bandalart.core.designsystem.generated.resources.weekly_review_title
import bandalart.core.designsystem.generated.resources.weekly_review_total
import com.nexters.bandalart.core.common.extension.toColor
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily
import com.nexters.bandalart.core.domain.entity.CompletionHeatmap
import com.nexters.bandalart.core.domain.entity.WeeklyReviewBandalartSummary
import com.nexters.bandalart.core.domain.entity.WeeklyReviewSummary
import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.home.model.BandalartUiModel
import com.nexters.bandalart.feature.home.ui.bottomSheetContentWindowInsets
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WeeklyReviewBottomSheet(
    summary: WeeklyReviewSummary,
    heatmap: CompletionHeatmap,
    bandalartList: ImmutableList<BandalartUiModel>,
    onHomeUiAction: (HomeScreen.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        contentWindowInsets = { bottomSheetContentWindowInsets() },
        onDismissRequest = { onHomeUiAction(HomeScreen.Event.DismissBottomSheet) },
        modifier = modifier,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
        ) {
            WeeklyReviewHeader(
                summary = summary,
                onCloseClick = { onHomeUiAction(HomeScreen.Event.DismissBottomSheet) },
            )
            if (summary.completedCount == 0) {
                Text(
                    text = stringResource(Res.string.weekly_review_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    fontFamily = pretendardFontFamily(),
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
            } else {
                Text(
                    text =
                        stringResource(
                            Res.string.weekly_review_total,
                            summary.completedCount,
                            summary.activeDayCount,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontFamily = pretendardFontFamily(),
                    fontWeight = FontWeight.W600,
                    letterSpacing = (-0.32).sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                summary.bandalarts.forEach { item ->
                    WeeklyReviewBandalartRow(
                        item = item,
                        title =
                            bandalartList.find { it.id == item.bandalartId }?.titleText
                                ?: item.title.orEmpty(),
                        onClick = { onHomeUiAction(HomeScreen.Event.SelectWeeklyReviewBandalart(item.bandalartId)) },
                    )
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            CompletionHeatmapSection(
                heatmap = heatmap,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun WeeklyReviewHeader(
    summary: WeeklyReviewSummary,
    onCloseClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(top = 10.dp),
        ) {
            Text(
                text = stringResource(Res.string.weekly_review_title),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontFamily = pretendardFontFamily(),
                fontWeight = FontWeight.W700,
                letterSpacing = (-0.4).sp,
                lineHeight = 28.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text =
                    stringResource(
                        Res.string.weekly_review_period,
                        summary.weekStart.monthNumber,
                        summary.weekStart.dayOfMonth,
                        summary.weekEnd.monthNumber,
                        summary.weekEnd.dayOfMonth,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontFamily = pretendardFontFamily(),
            )
        }
        IconButton(onClick = onCloseClick) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = stringResource(Res.string.clear_description),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun WeeklyReviewBandalartRow(
    item: WeeklyReviewBandalartSummary,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(item.mainColor.toColor()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.profileEmoji.orEmpty(),
                fontSize = 20.sp,
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontFamily = pretendardFontFamily(),
                fontWeight = FontWeight.W600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    stringResource(
                        Res.string.weekly_review_bandalart_progress,
                        item.completedCount,
                        item.completionRatio,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontFamily = pretendardFontFamily(),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}
