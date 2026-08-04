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

package com.nexters.bandalart.feature.home.model.dummy

import androidx.compose.ui.text.input.TextFieldValue
import com.nexters.bandalart.core.ui.allColor
import com.nexters.bandalart.feature.home.model.BandalartUiModel
import kotlin.time.Clock

val dummyBandalartList =
    listOf(
        BandalartUiModel(
            id = 0L,
            mainColor = allColor[0].mainColor,
            subColor = allColor[0].subColor,
            profileEmoji = "😎",
            title = TextFieldValue("발전하는 예진"),
            cellId = 0L,
            dueDate = Clock.System.now().toString(),
            isCompleted = false,
            completionRatio = 88,
            isGeneratedTitle = false,
        ),
        BandalartUiModel(
            id = 0L,
            mainColor = allColor[1].mainColor,
            subColor = allColor[1].subColor,
            profileEmoji = "🔥",
            title = TextFieldValue("발전하는 석규"),
            cellId = 0L,
            dueDate = Clock.System.now().toString(),
            isCompleted = false,
            completionRatio = 66,
            isGeneratedTitle = false,
        ),
        BandalartUiModel(
            id = 0L,
            mainColor = allColor[2].mainColor,
            subColor = allColor[2].subColor,
            profileEmoji = "❤️‍🔥",
            title = TextFieldValue("발전하는 지훈"),
            cellId = 0L,
            dueDate = Clock.System.now().toString(),
            isCompleted = false,
            completionRatio = 44,
            isGeneratedTitle = false,
        ),
        BandalartUiModel(
            id = 0L,
            mainColor = allColor[3].mainColor,
            subColor = allColor[3].subColor,
            profileEmoji = "💛",
            title = TextFieldValue("발전하는 인혁"),
            cellId = 0L,
            dueDate = Clock.System.now().toString(),
            isCompleted = false,
            completionRatio = 22,
            isGeneratedTitle = false,
        ),
    )
