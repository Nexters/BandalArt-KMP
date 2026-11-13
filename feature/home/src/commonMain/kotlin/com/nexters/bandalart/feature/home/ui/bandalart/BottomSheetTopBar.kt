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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexters.bandalart.feature.home.ui.bandalart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.clear_description
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.feature.home.model.CellType
import org.jetbrains.compose.resources.stringResource
import zone.ien.hig.CupertinoLiquidIconButton
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.adaptive.AdaptiveWidget
import zone.ien.hig.adaptive.ExperimentalAdaptiveApi
import zone.ien.hig.adaptive.Theme
import zone.ien.hig.adaptive.currentTheme
import zone.ien.hig.utils.rememberDefaultBackdrop
import androidx.compose.ui.tooling.preview.Preview

/** 닫기 버튼은 iOS(Cupertino)에서 48dp 유리 아이콘 버튼, 그 외는 기존 Material 아이콘 버튼으로 그린다. */
@OptIn(ExperimentalAdaptiveApi::class, ExperimentalCupertinoApi::class)
@Composable
fun BottomSheetTopBar(
    cellType: CellType,
    isBlankCell: Boolean,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        // iOS 닫기 버튼(48dp)이 제목보다 높아 제목을 세로 가운데에 둔다. Android는 기존 위치(위쪽)를 유지한다.
        contentAlignment = if (currentTheme == Theme.Cupertino) Alignment.Center else Alignment.TopStart,
    ) {
        BottomSheetTitleText(cellType = cellType, isBlankCell = isBlankCell)
        val closeIcon: @Composable () -> Unit = {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = stringResource(Res.string.clear_description),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        AdaptiveWidget(
            material = {
                IconButton(
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .height(21.dp)
                            .aspectRatio(1f),
                    onClick = onCloseClick,
                    content = closeIcon,
                )
            },
            cupertino = {
                CupertinoLiquidIconButton(
                    onClick = onCloseClick,
                    backdrop = rememberDefaultBackdrop(),
                    modifier = Modifier.align(Alignment.CenterEnd),
                    content = closeIcon,
                )
            },
        )
    }
}

// @ComponentPreview
@Preview
@Composable
private fun BottomSheetMainCellTopBarPreview() {
    BandalartTheme {
        BottomSheetTopBar(
            cellType = CellType.MAIN,
            isBlankCell = false,
            onCloseClick = {},
        )
    }
}

// @ComponentPreview
@Preview
@Composable
private fun BottomSheetSubCellTopBarPreview() {
    BandalartTheme {
        BottomSheetTopBar(
            cellType = CellType.SUB,
            isBlankCell = false,
            onCloseClick = {},
        )
    }
}

// @ComponentPreview
@Preview
@Composable
private fun BottomSheetBlankCellTopBarPreview() {
    BandalartTheme {
        BottomSheetTopBar(
            cellType = CellType.TASK,
            isBlankCell = true,
            onCloseClick = {},
        )
    }
}
