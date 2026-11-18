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

package com.nexters.bandalart.feature.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.delete_bandalart_dialog_empty_title
import bandalart.core.designsystem.generated.resources.delete_bandalart_dialog_message
import bandalart.core.designsystem.generated.resources.delete_bandalart_dialog_title
import bandalart.core.designsystem.generated.resources.delete_bandalart_maincell_dialog_message
import bandalart.core.designsystem.generated.resources.delete_bandalart_maincell_dialog_title
import bandalart.core.designsystem.generated.resources.delete_bandalart_subcell_dialog_message
import bandalart.core.designsystem.generated.resources.delete_bandalart_subcell_dialog_title
import bandalart.core.designsystem.generated.resources.delete_bandalart_taskcell_dialog_message
import bandalart.core.designsystem.generated.resources.delete_bandalart_taskcell_dialog_title
import bandalart.core.designsystem.generated.resources.routine_settings_reset_dialog_empty_title
import bandalart.core.designsystem.generated.resources.routine_settings_reset_dialog_title
import bandalart.core.designsystem.generated.resources.widget_guide_later
import bandalart.core.designsystem.generated.resources.widget_guide_message
import bandalart.core.designsystem.generated.resources.widget_guide_show_steps
import bandalart.core.designsystem.generated.resources.widget_guide_steps_android
import bandalart.core.designsystem.generated.resources.widget_guide_steps_close
import bandalart.core.designsystem.generated.resources.widget_guide_steps_confirm
import bandalart.core.designsystem.generated.resources.widget_guide_steps_ios
import bandalart.core.designsystem.generated.resources.widget_guide_steps_title
import bandalart.core.designsystem.generated.resources.widget_guide_title
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily
import com.nexters.bandalart.core.domain.widget.HomeWidgetPlatform
import com.nexters.bandalart.feature.home.model.BandalartUiModel
import com.nexters.bandalart.feature.home.model.CellType
import com.nexters.bandalart.feature.home.ui.bandalart.BandalartActionAlertDialog
import com.nexters.bandalart.feature.home.ui.bandalart.BandalartDeleteAlertDialog
import com.nexters.bandalart.feature.home.ui.bandalart.RewardedBandalartAlertDialog
import com.nexters.bandalart.feature.home.ui.bandalart.ResetCompletionsAlertDialog
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeDialogs(
    dialog: HomeScreen.DialogState?,
    bandalartData: BandalartUiModel?,
    eventSink: (HomeScreen.Event) -> Unit,
) {
    when (dialog) {
        is HomeScreen.DialogState.BandalartDelete -> {
            bandalartData?.let { bandalart ->
                BandalartDeleteAlertDialog(
                    title =
                        if (bandalart.titleText.isEmpty()) {
                            stringResource(Res.string.delete_bandalart_dialog_empty_title)
                        } else {
                            stringResource(Res.string.delete_bandalart_dialog_title, bandalart.titleText)
                        },
                    message = stringResource(Res.string.delete_bandalart_dialog_message),
                    onDeleteClick = {
                        eventSink(HomeScreen.Event.DeleteBandalart(bandalart.id))
                    },
                    onCancelClick = {
                        eventSink(HomeScreen.Event.DismissDialog)
                    },
                )
            }
        }
        HomeScreen.DialogState.RewardedCreate -> {
            RewardedBandalartAlertDialog(
                onConfirmClick = {
                    eventSink(HomeScreen.Event.ConfirmRewardedCreate)
                },
                onCancelClick = {
                    eventSink(HomeScreen.Event.DismissDialog)
                },
            )
        }
        is HomeScreen.DialogState.ResetCompletions -> {
            ResetCompletionsAlertDialog(
                title =
                    if (dialog.bandalartTitle.isEmpty()) {
                        stringResource(Res.string.routine_settings_reset_dialog_empty_title)
                    } else {
                        stringResource(Res.string.routine_settings_reset_dialog_title, dialog.bandalartTitle)
                    },
                onConfirmClick = {
                    eventSink(HomeScreen.Event.ConfirmResetCompletions(dialog.bandalartId))
                },
                onCancelClick = {
                    eventSink(HomeScreen.Event.DismissDialog)
                },
            )
        }
        is HomeScreen.DialogState.CellDelete -> {
            BandalartDeleteAlertDialog(
                title =
                    when (dialog.cellType) {
                        CellType.MAIN -> stringResource(Res.string.delete_bandalart_maincell_dialog_title, dialog.cellTitle ?: "")
                        CellType.SUB -> stringResource(Res.string.delete_bandalart_subcell_dialog_title, dialog.cellTitle ?: "")
                        else -> stringResource(Res.string.delete_bandalart_taskcell_dialog_title, dialog.cellTitle ?: "")
                    },
                message =
                    when (dialog.cellType) {
                        CellType.MAIN -> stringResource(Res.string.delete_bandalart_maincell_dialog_message)
                        CellType.SUB -> stringResource(Res.string.delete_bandalart_subcell_dialog_message)
                        else -> stringResource(Res.string.delete_bandalart_taskcell_dialog_message)
                    },
                onDeleteClick = {
                    eventSink(HomeScreen.Event.DeleteCell(dialog.cellId))
                },
                onCancelClick = {
                    eventSink(HomeScreen.Event.DismissDialog)
                },
            )
        }
        is HomeScreen.DialogState.WidgetGuide -> {
            BandalartActionAlertDialog(
                icon = null,
                iconContentDescription = null,
                title = stringResource(Res.string.widget_guide_title),
                message = stringResource(Res.string.widget_guide_message),
                confirmLabel = stringResource(Res.string.widget_guide_show_steps),
                cancelLabel = stringResource(Res.string.widget_guide_later),
                onConfirmClick = {
                    eventSink(HomeScreen.Event.ShowWidgetGuideSteps)
                },
                onCancelClick = {
                    eventSink(HomeScreen.Event.DismissDialog)
                },
            )
        }
        is HomeScreen.DialogState.WidgetGuideSteps -> {
            BandalartActionAlertDialog(
                icon = null,
                iconContentDescription = null,
                title = stringResource(Res.string.widget_guide_steps_title),
                message = null,
                confirmLabel = stringResource(Res.string.widget_guide_steps_confirm),
                cancelLabel = stringResource(Res.string.widget_guide_steps_close),
                onConfirmClick = {
                    eventSink(HomeScreen.Event.DismissDialog)
                },
                onCancelClick = {
                    eventSink(HomeScreen.Event.DismissDialog)
                },
                content = {
                    Text(
                        text =
                            when (dialog.platform) {
                                HomeWidgetPlatform.ANDROID -> stringResource(Res.string.widget_guide_steps_android)
                                HomeWidgetPlatform.IOS -> stringResource(Res.string.widget_guide_steps_ios)
                            },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontFamily = pretendardFontFamily(),
                        fontWeight = FontWeight.W500,
                        lineHeight = 24.sp,
                    )
                },
            )
        }
        null -> {}
    }
}
