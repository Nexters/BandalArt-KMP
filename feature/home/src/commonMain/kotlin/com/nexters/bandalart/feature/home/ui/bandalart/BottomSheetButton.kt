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

package com.nexters.bandalart.feature.home.ui.bandalart

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.unit.dp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.bottomsheet_delete
import bandalart.core.designsystem.generated.resources.bottomsheet_done
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.designsystem.theme.Gray200
import com.nexters.bandalart.core.designsystem.theme.Gray400
import com.nexters.bandalart.core.designsystem.theme.Gray900
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun BottomSheetDeleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = IconButtonColors(Gray200, Gray900, Gray200, Gray900),
    ) {
        BottomSheetButtonText(
            text = stringResource(Res.string.bottomsheet_delete),
            color = Gray900,
        )
    }
}

@Composable
fun BottomSheetCompleteButton(
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = IconButtonColors(Gray900, White, Gray200, Gray400),
        enabled = isEnabled,
    ) {
        BottomSheetButtonText(
            text = stringResource(Res.string.bottomsheet_done),
            color = if (isEnabled) White else Gray400,
        )
    }
}

// @ComponentPreview
@Preview
@Composable
private fun BottomSheetDeleteButtonPreview() {
    BandalartTheme {
        BottomSheetDeleteButton(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}

// @ComponentPreview
@Preview
@Composable
private fun BottomSheetCompleteButtonPreview() {
    BandalartTheme {
        BottomSheetCompleteButton(
            isEnabled = false,
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}
