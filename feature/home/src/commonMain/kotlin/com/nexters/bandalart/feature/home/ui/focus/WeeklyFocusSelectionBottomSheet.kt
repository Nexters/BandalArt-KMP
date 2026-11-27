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

package com.nexters.bandalart.feature.home.ui.focus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.clear_description
import bandalart.core.designsystem.generated.resources.weekly_focus_clear_all
import bandalart.core.designsystem.generated.resources.weekly_focus_pick
import bandalart.core.designsystem.generated.resources.weekly_focus_selection_body
import bandalart.core.designsystem.generated.resources.weekly_focus_selection_count
import bandalart.core.designsystem.generated.resources.weekly_focus_selection_empty
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily
import com.nexters.bandalart.core.domain.policy.WEEKLY_FOCUS_TASK_LIMIT
import com.nexters.bandalart.core.ui.component.BandalartSheetContent
import com.nexters.bandalart.core.ui.component.bandalartSheetContainerColor
import com.nexters.bandalart.core.ui.component.bandalartSheetWindowInsets
import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.home.ui.bandalart.BottomSheetCompleteButton
import com.nexters.bandalart.feature.home.ui.bottomSheetContentWindowInsets
import org.jetbrains.compose.resources.stringResource
import zone.ien.hig.adaptive.AdaptiveIconButton
import zone.ien.hig.adaptive.ExperimentalAdaptiveApi

/** 현재 반다라트의 할 일 칸 중 이번 주에 할 일을 최대 3개 고른다. 완료를 눌러야 저장된다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WeeklyFocusSelectionBottomSheet(
    state: HomeScreen.BottomSheetState.WeeklyFocusSelection,
    onHomeUiAction: (HomeScreen.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isLimitReached = state.selectedIds.size >= WEEKLY_FOCUS_TASK_LIMIT

    ModalBottomSheet(
        contentWindowInsets = { bandalartSheetWindowInsets(bottomSheetContentWindowInsets()) },
        onDismissRequest = { onHomeUiAction(HomeScreen.Event.DismissBottomSheet) },
        modifier = modifier,
        sheetState = sheetState,
        containerColor = bandalartSheetContainerColor(),
        dragHandle = null,
    ) {
        BandalartSheetContent(insets = bottomSheetContentWindowInsets()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp),
            ) {
                WeeklyFocusSelectionHeader(
                    selectedCount = state.selectedIds.size,
                    canClear = state.selectedIds.isNotEmpty(),
                    onClearClick = { onHomeUiAction(HomeScreen.Event.ClearWeeklyFocusSelection) },
                    onCloseClick = { onHomeUiAction(HomeScreen.Event.DismissBottomSheet) },
                )
                Text(
                    text =
                        stringResource(
                            if (state.candidates.isEmpty()) {
                                Res.string.weekly_focus_selection_empty
                            } else {
                                Res.string.weekly_focus_selection_body
                            },
                            WEEKLY_FOCUS_TASK_LIMIT,
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontFamily = pretendardFontFamily(),
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = CANDIDATE_LIST_MAX_HEIGHT)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp),
                ) {
                    state.candidates.forEach { candidate ->
                        val isSelected = candidate.cell.id in state.selectedIds
                        WeeklyFocusTaskRow(
                            title = candidate.cell.title.orEmpty(),
                            subtitle = candidate.subGoalTitle,
                            isChecked = isSelected,
                            isCompletedStyle = candidate.cell.isCompleted,
                            isEnabled = isSelected || !isLimitReached,
                            onClick = {
                                onHomeUiAction(HomeScreen.Event.ToggleWeeklyFocusSelection(candidate.cell.id))
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                BottomSheetCompleteButton(
                    isEnabled = true,
                    onClick = { onHomeUiAction(HomeScreen.Event.SaveWeeklyFocusSelection) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalAdaptiveApi::class)
@Composable
private fun WeeklyFocusSelectionHeader(
    selectedCount: Int,
    canClear: Boolean,
    onClearClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.weekly_focus_pick),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontFamily = pretendardFontFamily(),
            fontWeight = FontWeight.W700,
            letterSpacing = (-0.4).sp,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(Res.string.weekly_focus_selection_count, selectedCount, WEEKLY_FOCUS_TASK_LIMIT),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 15.sp,
            fontFamily = pretendardFontFamily(),
            fontWeight = FontWeight.W600,
            modifier = Modifier.weight(1f),
        )
        if (canClear) {
            Text(
                text = stringResource(Res.string.weekly_focus_clear_all),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontFamily = pretendardFontFamily(),
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = onClearClick)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
        AdaptiveIconButton(onClick = onCloseClick) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = stringResource(Res.string.clear_description),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private val CANDIDATE_LIST_MAX_HEIGHT = 360.dp
