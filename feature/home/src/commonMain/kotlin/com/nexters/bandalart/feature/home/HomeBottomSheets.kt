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

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.TextFieldValue
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.bandalart_list_empty_title
import com.nexters.bandalart.core.domain.entity.ThemeMode
import com.nexters.bandalart.core.domain.notification.DeadlineNotificationAuthorizationStatus
import com.nexters.bandalart.core.domain.notification.DeadlineReminderSchedulingHealth
import com.nexters.bandalart.feature.home.model.BandalartUiModel
import com.nexters.bandalart.feature.home.ui.bandalart.BandalartBottomSheet
import com.nexters.bandalart.feature.home.ui.bandalart.BandalartEmojiBottomSheet
import com.nexters.bandalart.feature.home.ui.bandalart.BandalartListBottomSheet
import com.nexters.bandalart.feature.home.ui.settings.SettingsBottomSheet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeBottomSheets(
    bottomSheet: HomeScreen.BottomSheetState?,
    recentEmojis: ImmutableList<String>,
    bandalartList: ImmutableList<BandalartUiModel>,
    themeMode: ThemeMode,
    deadlineReminderEnabled: Boolean,
    deadlineNotificationAuthorizationStatus: DeadlineNotificationAuthorizationStatus,
    deadlineReminderSchedulingHealth: DeadlineReminderSchedulingHealth,
    eventSink: (HomeScreen.Event) -> Unit,
    appVersion: String,
) {
    when (bottomSheet) {
        is HomeScreen.BottomSheetState.Cell -> {
            BandalartBottomSheet(
                cellType = bottomSheet.cellType,
                isBlankCell = bottomSheet.initialCellData.title.isNullOrEmpty(),
                onHomeUiAction = eventSink,
                bottomSheetData = bottomSheet,
                recentEmojis = recentEmojis,
            )
        }

        is HomeScreen.BottomSheetState.Emoji -> {
            BandalartEmojiBottomSheet(
                bandalartId = bottomSheet.bandalartId,
                cellId = bottomSheet.cellId,
                currentEmoji = bottomSheet.currentEmoji,
                recentEmojis = recentEmojis,
                onHomeUiAction = eventSink,
            )
        }

        is HomeScreen.BottomSheetState.BandalartList -> {
            BandalartListBottomSheet(
                bandalartList = updateBandalartListTitles(bandalartList).toImmutableList(),
                currentBandalartId = bottomSheet.currentBandalartId,
                isCreationOptionsVisible = bottomSheet.isCreationOptionsVisible,
                onHomeUiAction = eventSink,
            )
        }

        HomeScreen.BottomSheetState.Settings -> {
            SettingsBottomSheet(
                themeMode = themeMode,
                deadlineReminderEnabled = deadlineReminderEnabled,
                deadlineNotificationAuthorizationStatus = deadlineNotificationAuthorizationStatus,
                deadlineReminderSchedulingHealth = deadlineReminderSchedulingHealth,
                appVersion = appVersion,
                onHomeUiAction = eventSink,
            )
        }

        null -> {}
    }
}

@Composable
private fun updateBandalartListTitles(list: List<BandalartUiModel>): List<BandalartUiModel> {
    var counter = 1
    return list.map { item ->
        if (!item.hasTitleText) {
            val updatedTitle = stringResource(Res.string.bandalart_list_empty_title, counter)
            counter += 1
            item.copy(
                title = TextFieldValue(updatedTitle),
                isGeneratedTitle = true,
            )
        } else {
            item
        }
    }
}
