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

package com.nexters.bandalart.feature.home.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import bandalart.core.designsystem.generated.resources.settings_app_info
import bandalart.core.designsystem.generated.resources.settings_appearance
import bandalart.core.designsystem.generated.resources.settings_contact
import bandalart.core.designsystem.generated.resources.settings_theme_dark
import bandalart.core.designsystem.generated.resources.settings_theme_light
import bandalart.core.designsystem.generated.resources.settings_theme_system
import bandalart.core.designsystem.generated.resources.settings_title
import bandalart.core.designsystem.generated.resources.settings_version
import bandalart.core.designsystem.generated.resources.settings_version_value
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily
import com.nexters.bandalart.core.domain.entity.ThemeMode
import com.nexters.bandalart.feature.home.HomeScreen
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsBottomSheet(
    themeMode: ThemeMode,
    appVersion: String,
    onHomeUiAction: (HomeScreen.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { onHomeUiAction(HomeScreen.Event.DismissBottomSheet) },
        modifier = modifier.statusBarsPadding(),
        sheetState = bottomSheetState,
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
            SettingsHeader(onCloseClick = { onHomeUiAction(HomeScreen.Event.DismissBottomSheet) })
            SettingsSectionTitle(text = stringResource(Res.string.settings_appearance))
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeMode.entries.forEach { mode ->
                    ThemeModeRow(
                        mode = mode,
                        selected = mode == themeMode,
                        onClick = { onHomeUiAction(HomeScreen.Event.SelectThemeMode(mode)) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            SettingsSectionTitle(text = stringResource(Res.string.settings_app_info))
            SettingsContactRow(
                onClick = { onHomeUiAction(HomeScreen.Event.ContactSupport) },
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.settings_version),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontFamily = pretendardFontFamily(),
                    fontWeight = FontWeight.W600,
                    letterSpacing = (-0.3).sp,
                    lineHeight = 21.sp,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(Res.string.settings_version_value, appVersion),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    fontFamily = pretendardFontFamily(),
                    letterSpacing = (-0.3).sp,
                    lineHeight = 21.sp,
                )
            }
        }
    }
}

@Composable
private fun SettingsContactRow(onClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(Res.string.settings_contact),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontFamily = pretendardFontFamily(),
            fontWeight = FontWeight.W600,
            letterSpacing = (-0.3).sp,
            lineHeight = 21.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsHeader(onCloseClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.settings_title),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontFamily = pretendardFontFamily(),
            fontWeight = FontWeight.W700,
            letterSpacing = (-0.4).sp,
            lineHeight = 28.sp,
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

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        fontFamily = pretendardFontFamily(),
        fontWeight = FontWeight.W600,
        letterSpacing = (-0.26).sp,
        lineHeight = 18.2.sp,
        modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 10.dp),
    )
}

@Composable
private fun ThemeModeRow(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val label =
        when (mode) {
            ThemeMode.SYSTEM -> stringResource(Res.string.settings_theme_system)
            ThemeMode.LIGHT -> stringResource(Res.string.settings_theme_light)
            ThemeMode.DARK -> stringResource(Res.string.settings_theme_dark)
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                ).clickable(onClick = onClick)
                .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontFamily = pretendardFontFamily(),
            fontWeight = if (selected) FontWeight.W600 else FontWeight.W400,
            letterSpacing = (-0.32).sp,
            lineHeight = 22.4.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors =
                RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.outline,
                ),
        )
    }
}
