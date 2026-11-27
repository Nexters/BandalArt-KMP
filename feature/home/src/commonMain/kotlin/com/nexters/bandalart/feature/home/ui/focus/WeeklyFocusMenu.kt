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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.weekly_focus_chip
import bandalart.core.designsystem.generated.resources.weekly_focus_edit
import bandalart.core.designsystem.generated.resources.weekly_focus_progress
import bandalart.core.designsystem.generated.resources.weekly_focus_title
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily
import com.nexters.bandalart.core.domain.policy.WeeklyFocusTask
import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.home.ui.GLASS_CONTROL_HEIGHT
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import zone.ien.hig.CupertinoDropdownMenu
import zone.ien.hig.CupertinoLiquidButton
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.MenuAction
import zone.ien.hig.MenuDivider
import zone.ien.hig.MenuPickerAction
import zone.ien.hig.MenuTitle
import zone.ien.hig.adaptive.AdaptiveWidget
import zone.ien.hig.adaptive.ExperimentalAdaptiveApi

/**
 * 홈 상단 바에 이번 주에 고른 할 일 수를 칩으로 보이고, 누르면 표 위에 겹쳐 목록을 펼친다.
 * 레이아웃 높이를 쓰지 않으므로 표 위치가 바뀌지 않는다. 체크하면 원래 칸의 완료를 바꾼다.
 */
@OptIn(ExperimentalAdaptiveApi::class, ExperimentalCupertinoApi::class)
@Composable
internal fun WeeklyFocusMenu(
    tasks: ImmutableList<WeeklyFocusTask>,
    onHomeUiAction: (HomeScreen.Event) -> Unit,
    backdrop: LayerBackdrop,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val completedCount = tasks.count { it.cell.isCompleted }
    val chipText = stringResource(Res.string.weekly_focus_chip, completedCount, tasks.size)
    val title = stringResource(Res.string.weekly_focus_title)
    val progress = stringResource(Res.string.weekly_focus_progress, completedCount, tasks.size)
    val editText = stringResource(Res.string.weekly_focus_edit)
    val onToggle = { task: WeeklyFocusTask -> onHomeUiAction(HomeScreen.Event.ToggleTaskCompletion(task.cell)) }
    val onEdit = {
        isExpanded = false
        onHomeUiAction(HomeScreen.Event.OpenWeeklyFocusSelection)
    }

    Box(modifier = modifier) {
        AdaptiveWidget(
            material = {
                Row(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(role = Role.DropdownList) { isExpanded = true }
                            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WeeklyFocusChipLabel(text = chipText, color = contentColor)
                }
                DropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false },
                    modifier =
                        Modifier
                            .widthIn(min = 240.dp, max = 320.dp)
                            .background(MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontFamily = pretendardFontFamily(),
                            fontWeight = FontWeight.W700,
                            letterSpacing = (-0.3).sp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = progress,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontFamily = pretendardFontFamily(),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = editText,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontFamily = pretendardFontFamily(),
                            fontWeight = FontWeight.W600,
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(role = Role.Button, onClick = onEdit)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                    tasks.forEach { task ->
                        WeeklyFocusTaskRow(
                            title = task.cell.title.orEmpty(),
                            isChecked = task.cell.isCompleted,
                            isCompletedStyle = task.cell.isCompleted,
                            onClick = { onToggle(task) },
                        )
                    }
                }
            },
            cupertino = {
                CupertinoLiquidButton(
                    onClick = { isExpanded = true },
                    backdrop = backdrop,
                    contentPadding = PaddingValues(start = 14.dp, end = 8.dp),
                ) {
                    Box(modifier = Modifier.height(GLASS_CONTROL_HEIGHT), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            WeeklyFocusChipLabel(text = chipText, color = contentColor)
                        }
                    }
                }
                CupertinoDropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false },
                    backdrop = backdrop,
                ) {
                    MenuTitle { Text(text = "$title · $progress", fontFamily = pretendardFontFamily()) }
                    tasks.forEach { task ->
                        MenuPickerAction(
                            isSelected = task.cell.isCompleted,
                            onClick = { onToggle(task) },
                        ) {
                            Text(text = task.cell.title.orEmpty(), fontFamily = pretendardFontFamily())
                        }
                    }
                    MenuDivider()
                    MenuAction(onClick = onEdit) {
                        Text(text = editText, fontFamily = pretendardFontFamily())
                    }
                }
            },
        )
    }
}

@Composable
private fun RowScope.WeeklyFocusChipLabel(
    text: String,
    color: Color,
) {
    Text(
        text = text,
        color = color,
        fontSize = 14.sp,
        fontFamily = pretendardFontFamily(),
        fontWeight = FontWeight.W600,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f, fill = false),
    )
    Icon(
        imageVector = Icons.Default.ArrowDropDown,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(20.dp),
    )
}

@Composable
internal fun WeeklyFocusTaskRow(
    title: String,
    isChecked: Boolean,
    isCompletedStyle: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isEnabled: Boolean = true,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(enabled = isEnabled, role = Role.Checkbox, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WeeklyFocusCheckMark(isChecked = isChecked, isEnabled = isEnabled)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color =
                    if (isCompletedStyle || !isEnabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                fontSize = 15.sp,
                fontFamily = pretendardFontFamily(),
                fontWeight = FontWeight.W500,
                textDecoration = if (isCompletedStyle) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = pretendardFontFamily(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun WeeklyFocusCheckMark(
    isChecked: Boolean,
    isEnabled: Boolean,
) {
    val outlineColor =
        if (isEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier =
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .then(
                    if (isChecked) {
                        Modifier.background(MaterialTheme.colorScheme.primary)
                    } else {
                        Modifier.border(width = 1.5.dp, color = outlineColor, shape = CircleShape)
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (isChecked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
