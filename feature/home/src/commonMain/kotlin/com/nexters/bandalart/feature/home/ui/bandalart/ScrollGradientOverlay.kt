package com.nexters.bandalart.feature.home.ui.bandalart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

@Composable
internal fun ScrollGradientOverlay(
    isTop: Boolean,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isTop) listOf(White, Transparent) else listOf(Transparent, White),
                ),
                shape = RectangleShape,
            )
            .height(77.dp)
            .fillMaxWidth(),
    )
}
