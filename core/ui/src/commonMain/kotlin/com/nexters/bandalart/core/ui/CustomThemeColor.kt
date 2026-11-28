package com.nexters.bandalart.core.ui

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 사용자가 직접 고르는 테마 색상 계산.
 *
 * 플랫폼 색상 API(android.graphics.Color, UIColor) 대신 sRGB 정수 연산만 사용해서
 * Android와 iOS가 같은 입력에 같은 hex를 만든다.
 *
 * 색은 OKLCH(체감 밝기 L, 진하기 C, 색조 H)로 만든다. L이 사람이 느끼는 밝기에 가까워서
 * 같은 밝기 슬라이더 위치면 색조가 달라도 비슷한 밝기로 보인다.
 * - 밝기 하한 [MIN_LIGHTNESS]는 subColor(#111827) 글자와 대비 4.5:1(WCAG AA)을 모든 색조에서 지키는 값이다.
 * - 진하기는 [MAX_CHROMA]로 제한해 원색이 튀지 않게 하고, sRGB로 표현할 수 없으면 더 줄인다(gamut mapping).
 * - 대비 보장은 CustomThemeColorTest가 출력 hex 기준으로 검증한다.
 */
object CustomThemeColor {
    const val SUB_COLOR = "#111827"

    private const val TEXT_COLOR = 0x111827

    // WCAG AA 일반 텍스트 기준
    internal const val MIN_CONTRAST = 4.5

    // 진하기 상한 0.15에서 색조별로 필요한 하한의 최댓값(1° 간격, hex 반올림 포함 0.621)보다 조금 높게 둔다.
    private const val MIN_LIGHTNESS = 0.63

    // 2.8.0 HSL의 가장 연한 출력(체감 밝기 0.899~0.980)의 중간값. 더 올리면 흰 배경에서 셀이 거의 보이지 않는다.
    private const val MAX_LIGHTNESS = 0.93
    private const val MAX_CHROMA = 0.15

    // OKLCH에서 빨강은 약 29°에 있다. 슬라이더 0을 빨강으로 맞추기 위한 오프셋.
    private const val RED_HUE = 29.0

    private const val GAMUT_EPSILON = 1e-6
    private const val GRAY_CHROMA = 1e-4

    /** [hue] 0..360 (0 = 빨강), [tone] 0..1 (0 = 허용되는 가장 진한 색, 1 = 가장 연한 색) */
    fun toThemeColor(
        hue: Float,
        tone: Float,
    ): ThemeColor {
        val h = (hue.coerceIn(0f, 360f) + RED_HUE) % 360.0
        val lightness = MIN_LIGHTNESS + tone.coerceIn(0f, 1f) * (MAX_LIGHTNESS - MIN_LIGHTNESS)
        val chroma = maxChromaInGamut(lightness, h)
        return ThemeColor(mainColor = toHex(oklchToRgb(lightness, chroma, h)), subColor = SUB_COLOR)
    }

    /** 저장된 hex를 슬라이더 위치(hue, tone)로 되돌린다. 파싱할 수 없으면 null. */
    fun toHueTone(hex: String): Pair<Float, Float>? {
        val rgb = parseRgb(hex) ?: return null
        val (lightness, a, b) = rgbToOklab(rgb)
        // 회색은 색조가 정의되지 않으므로 슬라이더 0(빨강)으로 둔다.
        val okHue = if (sqrt(a * a + b * b) < GRAY_CHROMA) RED_HUE else (atan2(b, a) * 180 / PI + 360) % 360
        val hue = ((okHue - RED_HUE) % 360 + 360) % 360
        val tone = ((lightness - MIN_LIGHTNESS) / (MAX_LIGHTNESS - MIN_LIGHTNESS)).coerceIn(0.0, 1.0)
        return hue.toFloat() to tone.toFloat()
    }

    /** "#RRGGBB"와 "#AARRGGBB"를 같은 색으로 비교한다. */
    fun isSameColor(
        a: String,
        b: String,
    ): Boolean {
        val ra = parseRgb(a) ?: return false
        val rb = parseRgb(b) ?: return false
        return ra == rb
    }

    internal fun textContrast(hex: String): Double {
        val rgb = parseRgb(hex) ?: return 0.0
        return contrast(rgb, TEXT_COLOR)
    }

    private fun maxChromaInGamut(
        lightness: Double,
        hue: Double,
    ): Double {
        if (isInGamut(oklchToLinear(lightness, MAX_CHROMA, hue))) return MAX_CHROMA
        var low = 0.0
        var high = MAX_CHROMA
        repeat(24) {
            val mid = (low + high) / 2
            if (isInGamut(oklchToLinear(lightness, mid, hue))) low = mid else high = mid
        }
        return low
    }

    private fun isInGamut(linear: DoubleArray): Boolean = linear.all { it >= -GAMUT_EPSILON && it <= 1 + GAMUT_EPSILON }

    private fun oklchToRgb(
        lightness: Double,
        chroma: Double,
        hue: Double,
    ): Int {
        val (r, g, b) = oklchToLinear(lightness, chroma, hue).map { toSrgb(it.coerceIn(0.0, 1.0)) }
        return rgbOf(r, g, b)
    }

    private fun oklchToLinear(
        lightness: Double,
        chroma: Double,
        hue: Double,
    ): DoubleArray {
        val radians = hue * PI / 180
        val a = chroma * cos(radians)
        val b = chroma * sin(radians)
        val l = (lightness + 0.3963377774 * a + 0.2158037573 * b).pow(3)
        val m = (lightness - 0.1055613458 * a - 0.0638541728 * b).pow(3)
        val s = (lightness - 0.0894841775 * a - 1.2914855480 * b).pow(3)
        return doubleArrayOf(
            4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
        )
    }

    private fun rgbToOklab(rgb: Int): Triple<Double, Double, Double> {
        val r = toLinear(channel(rgb, 16))
        val g = toLinear(channel(rgb, 8))
        val b = toLinear(channel(rgb, 0))
        val l = cbrt(0.4122214708 * r + 0.5363083432 * g + 0.0514633806 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        return Triple(
            0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
        )
    }

    private fun parseRgb(hex: String): Int? {
        val digits = hex.removePrefix("#")
        val rgb =
            when (digits.length) {
                6 -> digits
                8 -> digits.substring(2)
                else -> return null
            }
        return rgb.toIntOrNull(16)
    }

    private fun toHex(rgb: Int): String = "#" + rgb.toString(16).uppercase().padStart(6, '0')

    private fun contrast(
        a: Int,
        b: Int,
    ): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun luminance(rgb: Int): Double =
        0.2126 * toLinear(channel(rgb, 16)) + 0.7152 * toLinear(channel(rgb, 8)) + 0.0722 * toLinear(channel(rgb, 0))

    private fun toLinear(channel: Int): Double {
        val v = channel / 255.0
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun toSrgb(linear: Double): Double = if (linear <= 0.0031308) 12.92 * linear else 1.055 * linear.pow(1 / 2.4) - 0.055

    private fun channel(
        rgb: Int,
        shift: Int,
    ): Int = (rgb shr shift) and 0xFF

    private fun rgbOf(
        r: Double,
        g: Double,
        b: Double,
    ): Int {
        fun to255(v: Double) = (v.coerceIn(0.0, 1.0) * 255).roundToInt()
        return (to255(r) shl 16) or (to255(g) shl 8) or to255(b)
    }
}
