package com.nexters.bandalart.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.unit.dp
import com.nexters.bandalart.core.common.extension.noRippleClickable
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val LOADING_ANIMATION_LOTTIE_FILE = "files/loading_animation.json"

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.noRippleClickable { },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .height(75.dp)
                    .aspectRatio(1f)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(90.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(White),
            ) {
                LottieImage(
                    jsonString = LOADING_ANIMATION_LOTTIE_FILE,
                    iterations = Int.MAX_VALUE,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp)
                )
            }
        }
    }
}

// @ComponentPreview
@Preview
@Composable
private fun LoadingIndicatorPreview() {
    BandalartTheme {
        LoadingIndicator()
    }
}
