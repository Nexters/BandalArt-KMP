package com.nexters.bandalart.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

/** 바텀 시트 뒤에 비칠 화면 기록. 콘텐츠를 `layerBackdrop`으로 기록하는 화면(iOS 홈)만 제공한다. */
val LocalSheetBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

@OptIn(ExperimentalAdaptiveApi::class)
@Composable
private fun sheetGlassBackdrop(): LayerBackdrop? = LocalSheetBackdrop.current.takeIf { currentTheme == Theme.Cupertino }

/** 시트 내용 최상위 배경색. Liquid Glass 시트면 면이 가려지지 않게 투명하게 둔다. */
@Composable
fun bandalartSheetBackgroundColor(): Color = if (sheetGlassBackdrop() != null) Color.Transparent else MaterialTheme.colorScheme.surface

/** `ModalBottomSheet`의 `contentWindowInsets`. Liquid Glass 시트면 inset을 [BandalartSheetContent] 안에서 주므로 비운다. */
@Composable
fun bandalartSheetWindowInsets(insets: WindowInsets): WindowInsets = if (sheetGlassBackdrop() != null) WindowInsets(0) else insets

/**
 * 바텀 시트 내용.
 * iOS(Cupertino)에서 [LocalSheetBackdrop]이 있으면 내용 뒤에 뒤 화면이 비치는 Liquid Glass 면을 그리고 [insets]를 안쪽 패딩으로 준다.
 * 시트가 올라오는 위치 이동보다 안쪽에서 그려야 면이 시트와 함께 움직이므로 `ModalBottomSheet`의 `modifier`가 아니라 내용에 둔다.
 * 그 외는 [content]를 그대로 그린다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandalartSheetContent(
    insets: WindowInsets,
    content: @Composable () -> Unit,
) {
    val backdrop = sheetGlassBackdrop()
    if (backdrop == null) {
        content()
        return
    }
    val shape = BottomSheetDefaults.ExpandedShape
    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = GLASS_SURFACE_ALPHA)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        colorControls(saturation = GLASS_SATURATION)
                        blur(24.dp.toPx())
                        lens(12.dp.toPx(), 24.dp.toPx())
                    },
                    highlight = { Highlight.Default.copy(alpha = GLASS_HIGHLIGHT_ALPHA) },
                    onDrawSurface = { drawRect(surfaceColor) },
                ),
    ) {
        Box(modifier = Modifier.windowInsetsPadding(insets)) {
            content()
        }
    }
}

private const val GLASS_SURFACE_ALPHA = 0.65f
private const val GLASS_SATURATION = 1.5f
private const val GLASS_HIGHLIGHT_ALPHA = 0.35f
