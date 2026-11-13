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
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.action_delete
import bandalart.core.designsystem.generated.resources.action_save
import bandalart.core.designsystem.generated.resources.delete_description
import bandalart.core.designsystem.generated.resources.ic_gallery
import bandalart.core.designsystem.generated.resources.ic_trash
import bandalart.core.designsystem.generated.resources.routine_settings_title
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.designsystem.theme.Error
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily
import com.nexters.bandalart.feature.home.HomeScreen
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import zone.ien.hig.CupertinoDropdownMenu
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.MenuAction
import zone.ien.hig.adaptive.AdaptiveWidget
import zone.ien.hig.adaptive.ExperimentalAdaptiveApi
import androidx.compose.ui.tooling.preview.Preview

/** iOS(Cupertino)는 hig Liquid Glass 메뉴, 그 외는 기존 Material 드롭다운으로 그린다. */
@OptIn(ExperimentalAdaptiveApi::class, ExperimentalCupertinoApi::class)
@Composable
fun BandalartDropDownMenu(
    isDropDownMenuOpened: Boolean,
    onAction: (HomeScreen.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onDismissRequest = { onAction(HomeScreen.Event.DismissDropDownMenu) }
    AdaptiveWidget(
        material = {
            DropdownMenu(
                modifier =
                    modifier
                        .wrapContentSize()
                        .background(MaterialTheme.colorScheme.surface),
                expanded = isDropDownMenuOpened,
                onDismissRequest = onDismissRequest,
                offset =
                    DpOffset(
                        x = (-18).dp,
                        y = 0.dp,
                    ),
                shape = RoundedCornerShape(12.dp),
            ) {
                DropdownMenuItem(
                    modifier =
                        Modifier
                            .wrapContentSize()
                            .padding(horizontal = 7.dp),
                    text = {
                        Row {
                            Icon(
                                imageVector = vectorResource(Res.drawable.ic_gallery),
                                contentDescription = "Gallery Icon",
                                modifier =
                                    Modifier
                                        .size(24.dp)
                                        .align(CenterVertically),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(Res.string.action_save),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W500,
                                modifier =
                                    Modifier
                                        .fillMaxHeight()
                                        .padding(start = 13.dp)
                                        .align(CenterVertically),
                                fontFamily = pretendardFontFamily(),
                            )
                        }
                    },
                    onClick = {
                        onAction(HomeScreen.Event.RequestSave)
                    },
                )
                Spacer(modifier = Modifier.height(2.dp))
                DropdownMenuItem(
                    modifier =
                        Modifier
                            .wrapContentSize()
                            .padding(horizontal = 7.dp),
                    text = {
                        Row {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier =
                                    Modifier
                                        .size(24.dp)
                                        .align(CenterVertically),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(Res.string.routine_settings_title),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W500,
                                modifier =
                                    Modifier
                                        .fillMaxHeight()
                                        .padding(start = 13.dp)
                                        .align(CenterVertically),
                                fontFamily = pretendardFontFamily(),
                            )
                        }
                    },
                    onClick = {
                        onAction(HomeScreen.Event.OpenRoutineSettings)
                    },
                )
                Spacer(modifier = Modifier.height(2.dp))
                DropdownMenuItem(
                    modifier =
                        Modifier
                            .wrapContentSize()
                            .padding(horizontal = 7.dp),
                    text = {
                        Row {
                            Icon(
                                imageVector = vectorResource(Res.drawable.ic_trash),
                                contentDescription = stringResource(Res.string.delete_description),
                                modifier =
                                    Modifier
                                        .size(24.dp)
                                        .align(CenterVertically),
                                tint = Color.Unspecified,
                            )
                            Text(
                                text = stringResource(Res.string.action_delete),
                                color = Error,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W500,
                                modifier =
                                    Modifier
                                        .fillMaxHeight()
                                        .padding(start = 13.dp)
                                        .align(CenterVertically),
                                fontFamily = pretendardFontFamily(),
                            )
                        }
                    },
                    onClick = {
                        onAction(HomeScreen.Event.OpenBandalartDeleteDialog)
                    },
                )
            }
        },
        cupertino = {
            // iOS 메뉴 관례대로 아이콘은 제목 오른쪽에 둔다.
            CupertinoDropdownMenu(
                expanded = isDropDownMenuOpened,
                onDismissRequest = onDismissRequest,
                modifier = modifier,
            ) {
                MenuAction(
                    onClick = { onAction(HomeScreen.Event.RequestSave) },
                    trailingIcon = {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_gallery),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                ) {
                    Text(text = stringResource(Res.string.action_save), fontFamily = pretendardFontFamily())
                }
                MenuAction(
                    onClick = { onAction(HomeScreen.Event.OpenRoutineSettings) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                ) {
                    Text(text = stringResource(Res.string.routine_settings_title), fontFamily = pretendardFontFamily())
                }
                MenuAction(
                    onClick = { onAction(HomeScreen.Event.OpenBandalartDeleteDialog) },
                    contentColor = Error,
                    trailingIcon = {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_trash),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = Color.Unspecified,
                        )
                    },
                ) {
                    Text(text = stringResource(Res.string.action_delete), fontFamily = pretendardFontFamily())
                }
            }
        },
    )
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
