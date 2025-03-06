package com.nexters.bandalart.feature.home.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexters.bandalart.core.designsystem.theme.Gray200

@Composable
fun CompletionRatioProgressBar(
    completionRatio: Int,
    progressColor: Color,
    modifier: Modifier = Modifier,
) {
    var progress by remember { mutableFloatStateOf(0f) }

    val size by animateFloatAsState(
        targetValue = progress,
        tween(
            durationMillis = 1000,
            delayMillis = 200,
            easing = LinearOutSlowInEasing,
        ),
    )

    LaunchedEffect(key1 = completionRatio) {
        progress = completionRatio / 100f
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize(),
    ) {
        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
        ) {
            // for the background of the ProgressBar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(5.dp))
                    .background(Gray200),
            )
            // for the progress of the ProgressBar
            Box(
                modifier = Modifier
                    .fillMaxWidth(size)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(progressColor)
                    .animateContentSize(),
            )
        }
    }
}

// @ComponentPreview
// @Composable
// private fun CompletionRatioProgressBarPreview() {
//     BandalartTheme {
//         CompletionRatioProgressBar(
//             completionRatio = 66,
//             progressColor = MainColor,
//         )
//     }
// }
