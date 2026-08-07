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

package com.nexters.bandalart.feature.home.ui.bandalart

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nexters.bandalart.core.ui.component.emoji.FluentEmojiPicker

@Composable
fun BandalartEmojiPicker(
    currentEmoji: String?,
    isBottomSheet: Boolean,
    onEmojiSelect: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FluentEmojiPicker(
        currentEmoji = currentEmoji,
        onEmojiSelect = onEmojiSelect,
        onClose = onClose,
        modifier = modifier,
        expanded = isBottomSheet,
    )
}
