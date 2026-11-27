package com.nexters.bandalart.core.ui

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 사용자가 직접 고르는 테마 색상 계산.
 *
 * 플랫폼 색상 API(android.graphics.Color, UIColor) 대신 sRGB 정수 연산만 사용해서
 * Android와 iOS가 같은 입력에 같은 hex를 만든다.
 *
 * subColor(#111827)는 고정이므로, 밝기 슬라이더의 하한을 색조마다 계산해서
 * mainColor 위 subColor 글자 대비가 WCAG AA(4.5:1) 이상이 되게 한다.
 * 파랑·보라·빨강처럼 원래 어두운 색조는 이 하한 때문에 노랑·초록보다 밝게 나온다.
 */
object CustomThemeColor {
    const val SUB_COLOR = "#111827"

    private const val TEXT_COLOR = 0x111827

    // WCAG AA 일반 텍스트 기준
    internal const val MIN_CONTRAST = 4.5

    private const val SATURATION = 0.8
    private const val MAX_LIGHTNESS = 0.92

    /** [hue] 0..360, [tone] 0..1 (0 = 허용되는 가장 진한 색, 1 = 가장 연한 색) */
    fun toThemeColor(
        hue: Float,
        tone: Float,
    ): ThemeColor {
        val h = hue.coerceIn(0f, 360f).toDouble()
        val minLightness = minLightness(h)
        val lightness = minLightness + tone.coerceIn(0f, 1f) * (MAX_LIGHTNESS - minLightness)
        return ThemeColor(mainColor = toHex(hslToRgb(h, SATURATION, lightness)), subColor = SUB_COLOR)
    }

    /** 저장된 hex를 슬라이더 위치(hue, tone)로 되돌린다. 파싱할 수 없으면 null. */
    fun toHueTone(hex: String): Pair<Float, Float>? {
        val rgb = parseRgb(hex) ?: return null
        val (h, _, l) = rgbToHsl(rgb)
        val minLightness = minLightness(h)
        val tone = ((l - minLightness) / (MAX_LIGHTNESS - minLightness)).coerceIn(0.0, 1.0)
        return h.toFloat() to tone.toFloat()
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

    private fun minLightness(hue: Double): Double {
        // 같은 hue·saturation에서 lightness가 오르면 휘도도 오르므로 이분 탐색이 성립한다.
        var low = 0.0
        var high = MAX_LIGHTNESS
        repeat(24) {
            val mid = (low + high) / 2
            val rgb = hslToRgb(hue, SATURATION, mid)
            if (contrast(rgb, TEXT_COLOR) >= MIN_CONTRAST) high = mid else low = mid
        }
        // hex 반올림으로 경계 아래로 떨어지지 않게 여유를 둔다.
        return min(high + 0.005, MAX_LIGHTNESS)
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

    private fun hslToRgb(
        h: Double,
        s: Double,
        l: Double,
    ): Int {
        val c = (1 - abs(2 * l - 1)) * s
        val x = c * (1 - abs((h / 60) % 2 - 1))
        val m = l - c / 2
        val (r, g, b) =
            when {
                h < 60 -> Triple(c, x, 0.0)
                h < 120 -> Triple(x, c, 0.0)
                h < 180 -> Triple(0.0, c, x)
                h < 240 -> Triple(0.0, x, c)
                h < 300 -> Triple(x, 0.0, c)
                else -> Triple(c, 0.0, x)
            }
        return rgbOf(r + m, g + m, b + m)
    }

    private fun rgbToHsl(rgb: Int): Triple<Double, Double, Double> {
        val r = channel(rgb, 16) / 255.0
        val g = channel(rgb, 8) / 255.0
        val b = channel(rgb, 0) / 255.0
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val l = (maxC + minC) / 2
        val d = maxC - minC
        if (d == 0.0) return Triple(0.0, 0.0, l)
        val s = d / (1 - abs(2 * l - 1))
        val h =
            when (maxC) {
                r -> 60 * (((g - b) / d) % 6)
                g -> 60 * ((b - r) / d + 2)
                else -> 60 * ((r - g) / d + 4)
            }
        return Triple((h + 360) % 360, s, l)
    }

    private fun contrast(
        a: Int,
        b: Int,
    ): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun luminance(rgb: Int): Double {
        fun linear(c: Int): Double {
            val v = c / 255.0
            return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear(channel(rgb, 16)) + 0.7152 * linear(channel(rgb, 8)) + 0.0722 * linear(channel(rgb, 0))
    }

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
