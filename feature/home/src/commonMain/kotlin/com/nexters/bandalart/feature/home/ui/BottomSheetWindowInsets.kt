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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo

/**
 * 하단 시트 내용에 줄 window inset.
 *
 * iPhone Duo처럼 가로 inset이 있는 화면에서 시트 내용이 상태 영역 밑으로 들어가지 않게 가로 inset을 더한다.
 * 다만 넓은 화면에서는 시트가 [BottomSheetDefaults.SheetMaxWidth]로 제한되어 가운데에 뜨므로,
 * 시트 바깥 여백이 가리는 만큼은 빼고 실제로 겹치는 너비만 패딩한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun bottomSheetContentWindowInsets(): WindowInsets {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val windowWidth = LocalWindowInfo.current.containerSize.width
    val sheetWidth = minOf(windowWidth, with(density) { BottomSheetDefaults.SheetMaxWidth.roundToPx() })
    val sideGap = (windowWidth - sheetWidth) / 2
    val safeDrawing = WindowInsets.safeDrawing
    return BottomSheetDefaults.windowInsets.union(
        WindowInsets(
            left = (safeDrawing.getLeft(density, layoutDirection) - sideGap).coerceAtLeast(0),
            right = (safeDrawing.getRight(density, layoutDirection) - sideGap).coerceAtLeast(0),
        ),
    )
}
