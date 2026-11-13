package com.nexters.bandalart.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import zone.ien.hig.adaptive.ExperimentalAdaptiveApi
import zone.ien.hig.adaptive.Theme
import zone.ien.hig.adaptive.currentTheme

/** 다이얼로그 뒤에 비칠 화면 기록. 콘텐츠를 `layerBackdrop`으로 기록하는 화면(iOS 홈)만 제공한다. */
val LocalDialogBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

private val DialogShape = RoundedCornerShape(16.dp)

/**
 * 알림 다이얼로그의 바탕 면.
 * iOS(Cupertino)에서 [LocalDialogBackdrop]이 있으면 뒤 화면이 비치는 Liquid Glass 면으로, 그 외는 기존 Material [Surface]로 그린다.
 */
@OptIn(ExperimentalAdaptiveApi::class)
@Composable
fun BandalartDialogSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val backdrop = LocalDialogBackdrop.current
    if (currentTheme == Theme.Cupertino && backdrop != null) {
        val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = GLASS_SURFACE_ALPHA)
        Box(
            modifier =
                modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { DialogShape },
                    effects = {
                        colorControls(saturation = GLASS_SATURATION)
                        blur(24.dp.toPx())
                        lens(12.dp.toPx(), 24.dp.toPx())
                    },
                    highlight = { Highlight.Default.copy(alpha = GLASS_HIGHLIGHT_ALPHA) },
                    onDrawSurface = { drawRect(surfaceColor) },
                ),
        ) {
            content()
        }
    } else {
        Surface(
            modifier = modifier,
            shape = DialogShape,
            color = MaterialTheme.colorScheme.surface,
            content = content,
        )
    }
}

private const val GLASS_SURFACE_ALPHA = 0.55f
private const val GLASS_SATURATION = 1.5f
private const val GLASS_HIGHLIGHT_ALPHA = 0.35f
