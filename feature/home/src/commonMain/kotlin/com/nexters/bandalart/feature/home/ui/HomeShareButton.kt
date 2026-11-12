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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.action_share
import bandalart.core.designsystem.generated.resources.ic_share
import bandalart.core.designsystem.generated.resources.share_description
import com.nexters.bandalart.core.common.extension.clickableSingle
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.nexters.bandalart.core.common.extension.rememberSingleClick
import zone.ien.hig.CupertinoLiquidButton
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.adaptive.AdaptiveWidget
import zone.ien.hig.adaptive.ExperimentalAdaptiveApi
import zone.ien.hig.utils.rememberDefaultBackdrop
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import androidx.compose.ui.tooling.preview.Preview

/** iOS(Cupertino)는 유리 버튼, 그 외는 기존 Material 버튼으로 그린다. [backdrop]은 유리 버튼이 굴절시킬 배경이다. */
@OptIn(ExperimentalAdaptiveApi::class, ExperimentalCupertinoApi::class)
@Composable
fun HomeShareButton(
    onShareButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop = rememberDefaultBackdrop(),
) {
    AdaptiveWidget(
        material = {
            Box(
                modifier =
                    modifier
                        .wrapContentSize()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickableSingle { onShareButtonClick() },
                contentAlignment = Alignment.Center,
            ) {
                HomeShareLabel(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 20.dp, bottom = 8.dp))
            }
        },
        cupertino = {
            // Material 쪽 clickableSingle처럼 중복 탭으로 공유가 두 번 열리지 않게 한다.
            CupertinoLiquidButton(
                onClick = rememberSingleClick(onShareButtonClick),
                backdrop = backdrop,
                modifier = modifier,
            ) {
                HomeShareLabel()
            }
        },
    )
}

@Composable
private fun HomeShareLabel(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.ic_share),
            contentDescription = stringResource(Res.string.share_description),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(Res.string.action_share),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.W700,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

// @ComponentPreview
@Preview
@Composable
private fun HomeShareButtonPreview() {
    BandalartTheme {
        HomeShareButton(
            onShareButtonClick = {},
        )
    }
}
