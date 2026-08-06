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

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.skeleton_trans_animate_label
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun BandalartSkeleton(modifier: Modifier = Modifier,) {
    val colorScheme = MaterialTheme.colorScheme
    val shimmerMainColors = listOf(colorScheme.surfaceVariant, colorScheme.outlineVariant, colorScheme.outline)
    val shimmerSubColors = listOf(colorScheme.surface, colorScheme.surfaceVariant, colorScheme.outlineVariant)
    val shimmerTaskColors = listOf(colorScheme.background, colorScheme.surface)

    val transition = rememberInfiniteTransition(label = "Skeleton transition")
    val translateAnim =
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 800f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = 600,
                            easing = FastOutLinearInEasing,
                        ),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = stringResource(Res.string.skeleton_trans_animate_label),
        )
    val mainBrush =
        Brush.linearGradient(
            colors = shimmerMainColors,
            start = Offset.Zero,
            end = Offset(x = translateAnim.value, y = translateAnim.value),
        )
    val subBrush =
        Brush.linearGradient(
            colors = shimmerSubColors,
            start = Offset.Zero,
            end = Offset(x = translateAnim.value, y = translateAnim.value),
        )
    val taskBrush =
        Brush.linearGradient(
            colors = shimmerTaskColors,
            start = Offset.Zero,
            end = Offset(x = translateAnim.value, y = translateAnim.value),
        )

    BandalartSkeletonScreen(
        taskBrush = taskBrush,
        subBrush = subBrush,
        mainBrush = mainBrush,
        modifier = modifier,
    )
}

// @ComponentPreview
@Preview
@Composable
private fun BandalartSkeletonPreview() {
    BandalartTheme {
        BandalartSkeleton()
    }
}
