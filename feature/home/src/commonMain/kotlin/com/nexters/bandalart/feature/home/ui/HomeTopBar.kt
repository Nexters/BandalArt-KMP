/*
 * Copyright 2025 easyhooon
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

package com.nexters.bandalart.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.add_description
import bandalart.core.designsystem.generated.resources.hamburger_description
import bandalart.core.designsystem.generated.resources.home_add
import bandalart.core.designsystem.generated.resources.home_list
import bandalart.core.designsystem.generated.resources.ic_hamburger
import bandalart.core.designsystem.generated.resources.ic_settings
import bandalart.core.designsystem.generated.resources.settings_description
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily
import com.nexters.bandalart.core.ui.component.AppTitle
import com.nexters.bandalart.feature.home.HomeScreen
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import androidx.compose.ui.tooling.preview.Preview

@Composable
internal fun HomeTopBar(
    bandalartCount: Int,
    onHomeUiAction: (HomeScreen.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(62.dp)
                .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(62.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTitle(
                modifier =
                    Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 20.dp, top = 2.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = { onHomeUiAction(HomeScreen.Event.OpenSettings) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_settings),
                    contentDescription = stringResource(Res.string.settings_description),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            Box(
                modifier =
                    Modifier
                        .padding(end = 20.dp)
                        .height(48.dp)
                        .widthIn(min = 48.dp)
                        .clickable(
                            onClick = { onHomeUiAction(HomeScreen.Event.OpenBandalartList) },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (bandalartCount > 1) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_hamburger),
                            contentDescription = stringResource(Res.string.hamburger_description),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(Res.string.home_list),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W700,
                            fontFamily = pretendardFontFamily(),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(Res.string.add_description),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(Res.string.home_add),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W700,
                        )
                    }
                }
            }
        }
    }
}

// @ComponentPreview
@Preview
@Composable
private fun HomeTopBarSingleBandalartPreview() {
    BandalartTheme {
        HomeTopBar(
            bandalartCount = 1,
            onHomeUiAction = {},
        )
    }
}

// @ComponentPreview
@Preview
@Composable
private fun HomeTopBarMultipleBandalartPreview() {
    BandalartTheme {
        HomeTopBar(
            bandalartCount = 2,
            onHomeUiAction = {},
        )
    }
}
