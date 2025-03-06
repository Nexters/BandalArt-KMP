package com.nexters.bandalart.feature.home.ui.bandalart

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color.Companion.White
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.skeleton_trans_animate_label
import com.nexters.bandalart.core.designsystem.theme.Gray100
import com.nexters.bandalart.core.designsystem.theme.Gray200
import com.nexters.bandalart.core.designsystem.theme.Gray300
import com.nexters.bandalart.core.designsystem.theme.Gray400
import com.nexters.bandalart.core.designsystem.theme.Gray50
import org.jetbrains.compose.resources.stringResource

private val shimmerMainColors = listOf(
    Gray200,
    Gray300,
    Gray400,
)

private val shimmerSubColors = listOf(
    Gray100,
    Gray200,
    Gray300,
)

private val shimmerTaskColors = listOf(
    White,
    Gray50,
)

@Composable
fun BandalartSkeleton(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "Skeleton transition")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                easing = FastOutLinearInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = stringResource(Res.string.skeleton_trans_animate_label),
    )
    val mainBrush = Brush.linearGradient(
        colors = shimmerMainColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value),
    )
    val subBrush = Brush.linearGradient(
        colors = shimmerSubColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value),
    )
    val taskBrush = Brush.linearGradient(
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
// @Composable
// private fun BandalartSkeletonPreview() {
//     BandalartTheme {
//         BandalartSkeleton()
//     }
// }
