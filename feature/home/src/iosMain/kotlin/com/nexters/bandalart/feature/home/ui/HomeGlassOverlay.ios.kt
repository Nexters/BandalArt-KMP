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

package com.nexters.bandalart.feature.home.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.action_share
import bandalart.core.designsystem.generated.resources.add_description
import bandalart.core.designsystem.generated.resources.hamburger_description
import bandalart.core.designsystem.generated.resources.home_add
import bandalart.core.designsystem.generated.resources.home_list
import bandalart.core.designsystem.generated.resources.ic_hamburger
import bandalart.core.designsystem.generated.resources.ic_settings
import bandalart.core.designsystem.generated.resources.ic_share
import bandalart.core.designsystem.generated.resources.settings_description
import bandalart.core.designsystem.generated.resources.share_description
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.nexters.bandalart.core.ui.component.AppTitle
import com.nexters.bandalart.feature.home.HomeScreen
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import zone.ien.hig.CupertinoIcon
import zone.ien.hig.CupertinoLiquidButton
import zone.ien.hig.CupertinoLiquidIconButton
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.theme.CupertinoTheme
import zone.ien.hig.theme.darkColorScheme
import zone.ien.hig.theme.lightColorScheme
import zone.ien.hig.utils.rememberDefaultBackdrop

internal actual val usesHomeGlassOverlay: Boolean = true

/** hig `CupertinoLiquidIconButton`의 고정 크기. */
private val GLASS_CONTROL_HEIGHT = 48.dp

@Composable
internal actual fun HomeGlassOverlay(
    bandalartCount: Int,
    isShareButtonVisible: Boolean,
    onHomeUiAction: (HomeScreen.Event) -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    // CupertinoTheme이 없으면 hig 색이 라이트로 고정되므로 앱 테마와 같은 기준으로 감싼다.
    val isDark = isSystemInDarkTheme()
    CupertinoTheme(colorScheme = if (isDark) darkColorScheme() else lightColorScheme()) {
        val backdrop = rememberDefaultBackdrop()
        Box(modifier = modifier) {
            // 유리 컨트롤이 굴절시킬 배경이므로 컨트롤은 기록 레이어 밖에 둔다.
            Box(modifier = Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                content()
            }
            HomeGlassTopBar(
                bandalartCount = bandalartCount,
                onHomeUiAction = onHomeUiAction,
                backdrop = backdrop,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            if (isShareButtonVisible) {
                HomeGlassShareButton(
                    onClick = { onHomeUiAction(HomeScreen.Event.RequestShare) },
                    backdrop = backdrop,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun HomeGlassTopBar(
    bandalartCount: Int,
    onHomeUiAction: (HomeScreen.Event) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(HOME_TOP_BAR_HEIGHT).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppTitle(modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        Spacer(modifier = Modifier.weight(1f))
        CupertinoLiquidIconButton(
            onClick = { onHomeUiAction(HomeScreen.Event.OpenSettings) },
            backdrop = backdrop,
        ) {
            CupertinoIcon(
                imageVector = vectorResource(Res.drawable.ic_settings),
                contentDescription = stringResource(Res.string.settings_description),
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        CupertinoLiquidButton(
            onClick = { onHomeUiAction(HomeScreen.Event.OpenBandalartList) },
            backdrop = backdrop,
            // 설정 아이콘 버튼(48dp)과 높이를 맞추려고 세로 패딩 대신 내용 높이를 고정한다.
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.height(GLASS_CONTROL_HEIGHT),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (bandalartCount > 1) {
                    CupertinoIcon(
                        imageVector = vectorResource(Res.drawable.ic_hamburger),
                        contentDescription = stringResource(Res.string.hamburger_description),
                    )
                    CupertinoText(text = stringResource(Res.string.home_list), fontWeight = FontWeight.W700)
                } else {
                    CupertinoIcon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(Res.string.add_description),
                        modifier = Modifier.size(20.dp),
                    )
                    CupertinoText(text = stringResource(Res.string.home_add), fontWeight = FontWeight.W700)
                }
            }
        }
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun HomeGlassShareButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    CupertinoLiquidButton(
        onClick = onClick,
        backdrop = backdrop,
        modifier = modifier,
    ) {
        CupertinoIcon(
            imageVector = vectorResource(Res.drawable.ic_share),
            contentDescription = stringResource(Res.string.share_description),
        )
        CupertinoText(
            text = stringResource(Res.string.action_share),
            fontWeight = FontWeight.W700,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
