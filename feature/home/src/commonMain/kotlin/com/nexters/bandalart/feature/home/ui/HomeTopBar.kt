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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.backdrops.LayerBackdrop
import zone.ien.hig.CupertinoLiquidButton
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.adaptive.AdaptiveIconButton
import zone.ien.hig.adaptive.AdaptiveWidget
import zone.ien.hig.adaptive.ExperimentalAdaptiveApi
import zone.ien.hig.adaptive.Theme
import zone.ien.hig.adaptive.currentTheme
import zone.ien.hig.utils.rememberDefaultBackdrop
import androidx.compose.ui.tooling.preview.Preview

/** 유리 상단 바 컨트롤 높이. hig `CupertinoLiquidIconButton`의 고정 크기와 같다. */
internal val HOME_TOP_BAR_HEIGHT = 62.dp
private val GLASS_CONTROL_HEIGHT = 48.dp

/**
 * iOS(Cupertino)는 배경 없이 콘텐츠 위에 뜨는 유리 컨트롤로, 그 외는 기존 Material 상단 바로 그린다.
 * [backdrop]은 유리 컨트롤이 굴절시킬 배경 기록 레이어다.
 */
@OptIn(ExperimentalAdaptiveApi::class, ExperimentalCupertinoApi::class)
@Composable
internal fun HomeTopBar(
    bandalartCount: Int,
    onHomeUiAction: (HomeScreen.Event) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop = rememberDefaultBackdrop(),
) {
    val isCupertino = currentTheme == Theme.Cupertino
    val contentColor = if (isCupertino) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(HOME_TOP_BAR_HEIGHT)
                .then(if (isCupertino) Modifier else Modifier.background(MaterialTheme.colorScheme.surface)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(HOME_TOP_BAR_HEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTitle(
                modifier =
                    Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 20.dp, top = 2.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            AdaptiveIconButton(
                onClick = { onHomeUiAction(HomeScreen.Event.OpenSettings) },
                modifier = Modifier.size(48.dp),
                adaptation = { cupertino { this.backdrop = backdrop } },
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_settings),
                    contentDescription = stringResource(Res.string.settings_description),
                    tint = contentColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            val onListClick = { onHomeUiAction(HomeScreen.Event.OpenBandalartList) }
            AdaptiveWidget(
                material = {
                    Box(
                        modifier =
                            Modifier
                                .padding(end = 20.dp)
                                .height(48.dp)
                                .widthIn(min = 48.dp)
                                .clickable(onClick = onListClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        HomeTopBarListLabel(bandalartCount = bandalartCount, color = contentColor)
                    }
                },
                cupertino = {
                    Spacer(modifier = Modifier.width(8.dp))
                    // 설정 아이콘 버튼과 높이를 맞추려고 세로 패딩 대신 내용 높이를 고정한다.
                    CupertinoLiquidButton(
                        onClick = onListClick,
                        backdrop = backdrop,
                        modifier = Modifier.padding(end = 16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        Box(modifier = Modifier.height(GLASS_CONTROL_HEIGHT), contentAlignment = Alignment.Center) {
                            HomeTopBarListLabel(bandalartCount = bandalartCount, color = contentColor)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun HomeTopBarListLabel(
    bandalartCount: Int,
    color: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (bandalartCount > 1) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_hamburger),
                contentDescription = stringResource(Res.string.hamburger_description),
                tint = color,
            )
            Text(
                text = stringResource(Res.string.home_list),
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.W700,
                fontFamily = pretendardFontFamily(),
            )
        } else {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(Res.string.add_description),
                tint = color,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(Res.string.home_add),
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.W700,
            )
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
