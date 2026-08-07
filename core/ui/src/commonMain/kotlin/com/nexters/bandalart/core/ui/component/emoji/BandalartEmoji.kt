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

package com.nexters.bandalart.core.ui.component.emoji

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.allDrawableResources
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
fun BandalartEmoji(
    unicode: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
) {
    val drawableResource =
        FluentEmojiCatalog
            .resourceKeyFor(unicode)
            ?.let(Res.allDrawableResources::get)

    if (drawableResource != null) {
        Image(
            painter = painterResource(drawableResource),
            contentDescription = contentDescription,
            modifier = modifier.size(size),
        )
    } else {
        val fallbackModifier =
            modifier.clearAndSetSemantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            }
        Text(
            text = unicode,
            modifier = fallbackModifier,
            fontSize = size.value.sp,
        )
    }
}
