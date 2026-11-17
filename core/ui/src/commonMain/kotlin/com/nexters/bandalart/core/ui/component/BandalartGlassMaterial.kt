package com.nexters.bandalart.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorFilter
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import zone.ien.hig.Accessibility
import zone.ien.hig.adaptive.ExperimentalAdaptiveApi
import zone.ien.hig.adaptive.Theme
import zone.ien.hig.adaptive.currentTheme
import zone.ien.hig.isReduceTransparencyEnabled
import zone.ien.hig.theme.CupertinoTheme

/**
 * 다이얼로그·시트 면을 Liquid Glass로 그릴지 여부.
 * iOS(Cupertino)에서만 쓰고, 접근성 "투명도 줄이기"가 켜져 있으면 hig 컴포넌트처럼 불투명 면으로 둔다.
 */
@OptIn(ExperimentalAdaptiveApi::class)
@Composable
internal fun isBandalartGlassEnabled(): Boolean = currentTheme == Theme.Cupertino && !Accessibility.isReduceTransparencyEnabled

/**
 * 네이티브 iOS alert·중간 높이 시트 재질에 맞춘 유리 면.
 * 측정값과 근거는 `docs/architecture/design-system/HIG_ADAPTIVE_LIQUID_GLASS_STRATEGY.md`의 "다이얼로그·시트 유리 재질 네이티브 맞춤"을 따른다.
 * 네이티브는 어두워진 배경에 회색 tint를 섞은 뒤 채도를 올린다. 기록된 화면은 어두워지기 전 값이라 backdrop에 dim 배율과 채도를 함께 건다.
 */
@Composable
internal fun Modifier.bandalartGlassSurface(
    backdrop: Backdrop,
    shape: Shape,
): Modifier {
    val isDark = CupertinoTheme.colorScheme.isDark
    val tint = if (isDark) DarkTint else LightTint
    val backdropFilter = if (isDark) DarkBackdropFilter else LightBackdropFilter
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            colorFilter(backdropFilter)
            blur(GLASS_BLUR_RADIUS.toPx())
            lens(GLASS_REFRACTION_HEIGHT.toPx(), GLASS_REFRACTION_AMOUNT.toPx())
        },
        highlight = { Highlight.Default.copy(alpha = GLASS_HIGHLIGHT_ALPHA) },
        onDrawSurface = { drawRect(tint) },
    )
}

/** 배경을 [dim] 배율로 어둡게 하고 Rec.709 휘도 기준으로 채도를 [GLASS_SATURATION]배 올리는 색 행렬. */
private fun dimAndSaturateFilter(dim: Float): ColorFilter {
    val s = GLASS_SATURATION
    val r = 0.2126f * (1f - s)
    val g = 0.7152f * (1f - s)
    val b = 0.0722f * (1f - s)
    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                dim * (r + s), dim * g, dim * b, 0f, 0f,
                dim * r, dim * (g + s), dim * b, 0f, 0f,
                dim * r, dim * g, dim * (b + s), 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
}

private const val GLASS_SATURATION = 1.7f
private const val GLASS_HIGHLIGHT_ALPHA = 0.35f
private val GLASS_BLUR_RADIUS = 30.dp
private val GLASS_REFRACTION_HEIGHT = 6.dp
private val GLASS_REFRACTION_AMOUNT = 8.dp

private val DarkTint = Color(0xFF5B5B5B).copy(alpha = 0.35f)
private val LightTint = Color.White.copy(alpha = 0.53f)
private val DarkBackdropFilter = dimAndSaturateFilter(dim = 0.52f)
private val LightBackdropFilter = dimAndSaturateFilter(dim = 0.80f)
