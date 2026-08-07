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

import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.feature.home.HomeScreen
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandalartEmojiBottomSheet(
    bandalartId: Long,
    cellId: Long,
    currentEmoji: String?,
    recentEmojis: List<String>,
    onHomeUiAction: (HomeScreen.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            onHomeUiAction(HomeScreen.Event.DismissBottomSheet)
        },
        modifier = modifier.wrapContentSize(),
        sheetState = bottomSheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        BandalartEmojiPicker(
            currentEmoji = currentEmoji,
            recentEmojis = recentEmojis,
            isBottomSheet = true,
            onEmojiSelect = { selectedEmoji ->
                onHomeUiAction(
                    HomeScreen.Event.UpdateBandalartEmoji(
                        bandalartId = bandalartId,
                        cellId = cellId,
                        emoji = selectedEmoji,
                    ),
                )
            },
        )
    }
}

// @ComponentPreview
@Preview
@Composable
private fun BandalartEmojiBottomSheetPreview() {
    BandalartTheme {
        BandalartEmojiBottomSheet(
            bandalartId = 0L,
            cellId = 0L,
            currentEmoji = "😎",
            recentEmojis = listOf("🎯", "🚀"),
            onHomeUiAction = {},
        )
    }
}
