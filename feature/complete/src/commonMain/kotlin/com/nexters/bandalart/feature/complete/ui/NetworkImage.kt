package com.nexters.bandalart.feature.complete.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.ic_app
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil3.CoilImage
import com.skydoves.landscapist.coil3.CoilImageState
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.placeholder.placeholder.PlaceholderPlugin
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.painterResource

@Composable
fun NetworkImage(
    imageUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: Painter? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (LocalInspectionMode.current) {
        Image(
            painter = painterResource(Res.drawable.ic_app),
            contentDescription = "Example Image Icon",
            modifier = modifier,
        )
    } else {
        CoilImage(
            imageModel = { imageUri },
            modifier = modifier,
            component = rememberImageComponent {
                +PlaceholderPlugin.Loading(placeholder)
                +PlaceholderPlugin.Failure(placeholder)
            },
            imageOptions = ImageOptions(
                contentScale = contentScale,
                alignment = Alignment.Center,
                contentDescription = contentDescription,
            ),
            onImageStateChanged = { state ->
                if (state is CoilImageState.Failure) {
                    Napier.e("CoilImageState.Failure", state.reason)
                }
            },
        )
    }
}

// @ComponentPreview
// @Composable
// private fun NetworkImagePreview() {
//     BandalartTheme {
//         NetworkImage(
//             imageUri = "",
//             contentDescription = "",
//         )
//     }
// }
