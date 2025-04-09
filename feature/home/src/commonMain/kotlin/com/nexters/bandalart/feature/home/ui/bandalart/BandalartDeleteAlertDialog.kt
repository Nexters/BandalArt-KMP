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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.delete_bandalart_cancel
import bandalart.core.designsystem.generated.resources.delete_bandalart_delete
import bandalart.core.designsystem.generated.resources.delete_description
import bandalart.core.designsystem.generated.resources.ic_delete
import com.nexters.bandalart.core.designsystem.theme.Gray200
import com.nexters.bandalart.core.designsystem.theme.Gray400
import com.nexters.bandalart.core.designsystem.theme.Gray900
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun BandalartDeleteAlertDialog(
    title: String,
    message: String?,
    onDeleteClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = { onCancelClick() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = White,
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_delete),
                    contentDescription = stringResource(Res.string.delete_description),
                    modifier = Modifier
                        .height(28.dp)
                        .align(Alignment.CenterHorizontally),
                    tint = Color.Unspecified,
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = title,
                    color = Gray900,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.W700,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp,
                    letterSpacing = (-0.4).sp,
                )
                if (message != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        color = Gray400,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.28).sp,
                    )
                }
                Spacer(modifier = Modifier.height(30.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        onClick = onCancelClick,
                        colors = ButtonColors(
                            containerColor = Gray200,
                            contentColor = Gray900,
                            disabledContainerColor = Gray200,
                            disabledContentColor = Gray900,
                        ),
                    ) {
                        Text(
                            text = stringResource(Res.string.delete_bandalart_cancel),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W600,
                            color = Gray900,
                        )
                    }
                    Spacer(modifier = Modifier.width(9.dp))
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        onClick = onDeleteClick,
                        colors = ButtonColors(
                            containerColor = Gray900,
                            contentColor = White,
                            disabledContainerColor = Gray900,
                            disabledContentColor = White,
                        ),
                    ) {
                        Text(
                            text = stringResource(Res.string.delete_bandalart_delete),
                            color = White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W600,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// @ComponentPreview
// @Composable
// private fun BandalartDeleteAlertDialogPreview() {
//     BandalartTheme {
//         BandalartDeleteAlertDialog(
//             title = "반다라트를 삭제하시겠어요?",
//             message = "삭제한 반다라트는 다시 복구할 수 없어요.",
//             onDeleteClick = {},
//             onCancelClick = {},
//         )
//     }
// }
