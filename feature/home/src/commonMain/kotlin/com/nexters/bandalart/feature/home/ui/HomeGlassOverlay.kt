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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexters.bandalart.feature.home.HomeScreen

internal val HOME_TOP_BAR_HEIGHT = 62.dp

/** true면 상단 바와 공유 버튼을 스크롤 영역 안에 두지 않고 [HomeGlassOverlay]가 콘텐츠 위에 띄운다. */
internal expect val usesHomeGlassOverlay: Boolean

/** iOS는 [content]를 배경으로 기록해 그 위에 Liquid Glass 상단 바와 공유 버튼을 띄운다. Android는 [content]만 그린다. */
@Composable
internal expect fun HomeGlassOverlay(
    bandalartCount: Int,
    isShareButtonVisible: Boolean,
    onHomeUiAction: (HomeScreen.Event) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
)
