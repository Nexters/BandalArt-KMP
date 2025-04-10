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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.delete_description
import bandalart.core.designsystem.generated.resources.dropdown_delete
import bandalart.core.designsystem.generated.resources.dropdown_save
import bandalart.core.designsystem.generated.resources.ic_gallery
import bandalart.core.designsystem.generated.resources.ic_trash
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.designsystem.theme.Error
import com.nexters.bandalart.core.designsystem.theme.Gray800
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily
import com.nexters.bandalart.feature.home.viewmodel.HomeUiAction
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun BandalartDropDownMenu(
    isDropDownMenuOpened: Boolean,
    onAction: (HomeUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        modifier = modifier
            .wrapContentSize()
            .background(White),
        expanded = isDropDownMenuOpened,
        onDismissRequest = {
            onAction(HomeUiAction.OnDropDownMenuDismiss)
        },
        offset = DpOffset(
            x = (-18).dp,
            y = 0.dp,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        DropdownMenuItem(
            modifier = Modifier
                .wrapContentSize()
                .padding(horizontal = 7.dp),
            text = {
                Row {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_gallery),
                        contentDescription = "Gallery Icon",
                        modifier = Modifier
                            .size(24.dp)
                            .align(CenterVertically),
                        tint = Color.Unspecified,
                    )
                    Text(
                        text = stringResource(Res.string.dropdown_save),
                        color = Gray800,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(start = 13.dp)
                            .align(CenterVertically),
                        fontFamily = pretendardFontFamily(),
                    )
                }
            },
            onClick = {
                onAction(HomeUiAction.OnSaveClick)
            },
        )
        Spacer(modifier = Modifier.height(2.dp))
        DropdownMenuItem(
            modifier = Modifier
                .wrapContentSize()
                .padding(horizontal = 7.dp),
            text = {
                Row {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_trash),
                        contentDescription = stringResource(Res.string.delete_description),
                        modifier = Modifier
                            .size(24.dp)
                            .align(CenterVertically),
                        tint = Color.Unspecified,
                    )
                    Text(
                        text = stringResource(Res.string.dropdown_delete),
                        color = Error,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(start = 13.dp)
                            .align(CenterVertically),
                        fontFamily = pretendardFontFamily(),
                    )
                }
            },
            onClick = {
                onAction(HomeUiAction.OnDeleteClick)
            },
        )
    }
}

// @ComponentPreview
@Preview
@Composable
private fun BandalartDropDownMenuPreview() {
    BandalartTheme {
        BandalartDropDownMenu(
            isDropDownMenuOpened = true,
            onAction = {},
        )
    }
}
