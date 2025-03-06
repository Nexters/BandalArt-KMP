package com.nexters.bandalart.core.ui.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import bandalart.core.designsystem.generated.resources.Res
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun LottieImage(
    jsonString: String,
    iterations: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes(jsonString).decodeToString()
        )
    }

    Image(
        painter = rememberLottiePainter(
            composition = composition,
            iterations = iterations,
        ),
        contentDescription = contentDescription,
        modifier = modifier
    )
}
