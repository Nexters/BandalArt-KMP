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

package com.nexters.bandalart.feature.home.model

import androidx.compose.ui.text.input.TextFieldValue

data class BandalartUiModel(
    val id: Long = 0L,
    val mainColor: String = "#3FFFBA",
    val subColor: String = "#111827",
    val profileEmoji: String? = "",
    val title: TextFieldValue? = TextFieldValue(""),
    val cellId: Long = 0L,
    val dueDate: String? = "",
    val isCompleted: Boolean = false,
    val completionRatio: Int = 0,
    val isGeneratedTitle: Boolean = false,
) {
    val titleText: String
        get() = title?.text ?: ""

    val hasTitleText: Boolean
        get() = titleText.isNotEmpty()
}
