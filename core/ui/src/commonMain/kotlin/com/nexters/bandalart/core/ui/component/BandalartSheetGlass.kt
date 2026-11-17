package com.nexters.bandalart.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.capsule.ContinuousRoundedRectangle

/** 바텀 시트 뒤에 비칠 화면 기록. 콘텐츠를 `layerBackdrop`으로 기록하는 화면(iOS 홈)만 제공한다. */
val LocalSheetBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/** 시트가 그린 면과 내용을 기록할 곳. 시트 위 다이얼로그가 바로 뒤의 시트를 비추도록 iOS 홈이 제공한다. */
val LocalSheetLayer = staticCompositionLocalOf<LayerBackdrop?> { null }

@Composable
private fun sheetGlassBackdrop(): LayerBackdrop? = LocalSheetBackdrop.current.takeIf { isBandalartGlassEnabled() }

/** 시트 내용 최상위 배경색. Liquid Glass 시트면 면이 가려지지 않게 투명하게 둔다. */
@Composable
fun bandalartSheetBackgroundColor(): Color = if (sheetGlassBackdrop() != null) Color.Transparent else MaterialTheme.colorScheme.surface

/**
 * `ModalBottomSheet`의 `containerColor`.
 * Liquid Glass 시트면 면을 [BandalartSheetContent]가 그리고, 떠 있는 시트의 바깥 여백이 비치도록 투명하게 둔다.
 */
@Composable
fun bandalartSheetContainerColor(): Color = if (sheetGlassBackdrop() != null) Color.Transparent else MaterialTheme.colorScheme.surface

/** 시트 안 강조 면(선택된 행 등)의 색. Liquid Glass 시트면 뒤가 비치도록 반투명하게 둔다. */
@Composable
fun bandalartSheetTintColor(color: Color): Color = if (sheetGlassBackdrop() != null) color.copy(alpha = GLASS_TINT_ALPHA) else color

/** `ModalBottomSheet`의 `contentWindowInsets`. Liquid Glass 시트면 inset을 [BandalartSheetContent] 안에서 주므로 비운다. */
@Composable
fun bandalartSheetWindowInsets(insets: WindowInsets): WindowInsets = if (sheetGlassBackdrop() != null) WindowInsets(0) else insets

/**
 * 바텀 시트 내용.
 * iOS(Cupertino)에서 [LocalSheetBackdrop]이 있으면 네이티브 시트처럼 그리고 [insets]를 안쪽 패딩으로 준다.
 * - 중간 높이: 화면 가장자리에서 떨어진 떠 있는 Liquid Glass 면.
 * - 화면 높이의 대부분을 차지하는 최대 높이: 가장자리에 붙은 불투명 면.
 * 시트가 올라오는 위치 이동보다 안쪽에서 그려야 면이 시트와 함께 움직이므로 `ModalBottomSheet`의 `modifier`가 아니라 내용에 둔다.
 * 그 외는 [content]를 그대로 그린다.
 */
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
    val sheetLayer = LocalSheetLayer.current
    val windowHeight = LocalWindowInfo.current.containerSize.height
    var sheetHeight by remember { mutableIntStateOf(0) }
    val isFullHeight = windowHeight > 0 && sheetHeight >= windowHeight * FULL_HEIGHT_FRACTION
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .onSizeChanged { sheetHeight = it.height }
                .then(
                    if (isFullHeight) {
                        Modifier
                    } else {
                        Modifier.padding(start = FLOATING_MARGIN, end = FLOATING_MARGIN, bottom = FLOATING_MARGIN)
                    },
                ).then(if (sheetLayer != null) Modifier.layerBackdrop(sheetLayer) else Modifier)
                .then(
                    if (isFullHeight) {
                        Modifier.background(bandalartFullHeightSheetColor(), FullHeightSheetShape)
                    } else {
                        Modifier.bandalartGlassSurface(backdrop = backdrop, shape = FloatingSheetShape)
                    },
                ),
    ) {
        Box(modifier = Modifier.windowInsetsPadding(insets)) {
            content()
        }
    }
}

/** 이 비율 이상 화면 높이를 차지하면 네이티브 최대 높이 시트처럼 불투명 면으로 그린다. */
private const val FULL_HEIGHT_FRACTION = 0.85f
private const val GLASS_TINT_ALPHA = 0.6f

/** 네이티브 중간 높이 시트의 좌우·아래 여백 (iOS 27.2 측정값 8pt). */
private val FLOATING_MARGIN = 8.dp

/** 네이티브 중간 높이 시트 모양. 위 36pt, 아래는 기기 모서리를 따라 50pt (iOS 27.2 측정값). */
private val FloatingSheetShape = ContinuousRoundedRectangle(36.dp, 36.dp, 50.dp, 50.dp)

/** 네이티브 최대 높이 시트 모양. 위 36pt, 아래는 화면 끝에 붙는다. */
private val FullHeightSheetShape = ContinuousRoundedRectangle(36.dp, 36.dp, 0.dp, 0.dp)
