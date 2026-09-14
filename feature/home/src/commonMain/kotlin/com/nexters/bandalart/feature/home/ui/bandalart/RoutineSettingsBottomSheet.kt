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

package com.nexters.bandalart.feature.home.ui.bandalart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.clear_description
import bandalart.core.designsystem.generated.resources.routine_settings_daily_reset_description
import bandalart.core.designsystem.generated.resources.routine_settings_daily_reset_title
import bandalart.core.designsystem.generated.resources.routine_settings_reset_now
import bandalart.core.designsystem.generated.resources.routine_settings_reset_now_description
import bandalart.core.designsystem.generated.resources.routine_settings_title
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily
import com.nexters.bandalart.feature.home.HomeScreen
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoutineSettingsBottomSheet(
    state: HomeScreen.BottomSheetState.RoutineSettings,
    onAction: (HomeScreen.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { onAction(HomeScreen.Event.DismissBottomSheet) },
        modifier = modifier,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
        ) {
            RoutineSettingsHeader(
                onCloseClick = { onAction(HomeScreen.Event.DismissBottomSheet) },
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.routine_settings_daily_reset_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontFamily = pretendardFontFamily(),
                        fontWeight = FontWeight.W600,
                        letterSpacing = (-0.32).sp,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(Res.string.routine_settings_daily_reset_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontFamily = pretendardFontFamily(),
                        lineHeight = 18.sp,
                        letterSpacing = (-0.26).sp,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = state.dailyResetEnabled,
                    onCheckedChange = { enabled ->
                        onAction(
                            HomeScreen.Event.SetDailyResetEnabled(
                                bandalartId = state.bandalartId,
                                enabled = enabled,
                            ),
                        )
                    },
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                Button(
                    onClick = { onAction(HomeScreen.Event.OpenResetCompletionsDialog) },
                    enabled = state.hasCompletedCells,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.routine_settings_reset_now),
                        fontFamily = pretendardFontFamily(),
                        fontWeight = FontWeight.W600,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.routine_settings_reset_now_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontFamily = pretendardFontFamily(),
                    lineHeight = 18.sp,
                    letterSpacing = (-0.26).sp,
                )
            }
        }
    }
}

@Composable
private fun RoutineSettingsHeader(onCloseClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.routine_settings_title),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontFamily = pretendardFontFamily(),
            fontWeight = FontWeight.W700,
            letterSpacing = (-0.4).sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onCloseClick) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = stringResource(Res.string.clear_description),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
