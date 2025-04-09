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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.unit.dp
import com.nexters.bandalart.core.common.extension.noRippleClickable
import com.nexters.bandalart.core.common.extension.toColor
import com.nexters.bandalart.core.designsystem.theme.Gray900
import com.nexters.bandalart.core.ui.ThemeColor
import com.nexters.bandalart.core.ui.allColor

@Composable
fun BandalartColorPicker(
    initColor: ThemeColor,
    onColorSelect: (ThemeColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(45.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        var initSelected by remember { mutableStateOf(initColor) }

        allColor.forEach {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f),
            ) {
                if (it.mainColor == initSelected.mainColor) {
                    Card(
                        border = BorderStroke(width = 1.5.dp, color = Gray900),
                        modifier = Modifier
                            .height(45.dp)
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(90.dp),
                        colors = CardDefaults.cardColors(White),
                    ) { }
                }
                Card(
                    modifier = Modifier
                        .height(36.dp)
                        .aspectRatio(1f)
                        .noRippleClickable {
                            initSelected = it
                            onColorSelect(initSelected)
                        },
                    shape = RoundedCornerShape(90.dp),
                    colors = CardDefaults.cardColors(containerColor = it.mainColor.toColor()),
                ) { }
            }
        }
    }
}

// @ComponentPreview
// @Composable
// private fun BandalartColorPickerPreview() {
//     BandalartTheme {
//         BandalartColorPicker(
//             initColor = ThemeColor("#3FFFBA", "#111827"),
//             onColorSelect = {},
//         )
//     }
// }
