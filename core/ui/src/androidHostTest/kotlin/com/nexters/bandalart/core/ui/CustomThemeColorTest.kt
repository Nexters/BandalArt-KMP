package com.nexters.bandalart.core.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CustomThemeColorTest {
    private val hexPattern = Regex("^#[0-9A-F]{6}$")

    @Test
    fun everyHueAndToneKeepsCellTextReadable() {
        // 슬라이더 값은 연속이므로 정수 각도 사이와 밝기 중간값까지 촘촘하게 확인한다.
        for (hueStep in 0..1440) {
            val hue = hueStep / 4f
            for (toneStep in 0..10) {
                val tone = toneStep / 10f
                val color = CustomThemeColor.toThemeColor(hue, tone)
                assertTrue(hexPattern.matches(color.mainColor), "hue=$hue tone=$tone -> ${color.mainColor}")
                assertTrue(
                    CustomThemeColor.textContrast(color.mainColor) >= CustomThemeColor.MIN_CONTRAST,
                    "hue=$hue tone=$tone -> ${color.mainColor}",
                )
                assertEquals(CustomThemeColor.SUB_COLOR, color.subColor)
            }
        }
    }

    @Test
    fun hueToneRoundTripsToSameColor() {
        for (hue in listOf(0f, 45f, 120f, 210f, 300f)) {
            for (tone in listOf(0f, 0.5f, 1f)) {
                val hex = CustomThemeColor.toThemeColor(hue, tone).mainColor
                val hueTone = CustomThemeColor.toHueTone(hex)
                assertNotNull(hueTone)
                val (h, t) = hueTone!!
                val restored = CustomThemeColor.toThemeColor(h, t).mainColor
                assertTrue(CustomThemeColor.isSameColor(hex, restored) || nearlySame(hex, restored), "$hex -> $restored")
            }
        }
    }

    @Test
    fun colorsNotMadeByGeneratorMapToValidSliderPosition() {
        // 프리셋, 2.8.0의 HSL 생성기가 만든 색, 회색처럼 지금 생성기로 다시 만들 수 없는 색도
        // 피커를 열 때 슬라이더 위치로 바뀌어야 한다.
        val inputs = allColor.map { it.mainColor } + listOf("#FF3FFFBA", "#E119E1", "#87870F", "#FBFBDA", "#FFFFFF", "#000000", "#808080", "#111827")
        for (hex in inputs) {
            val hueTone = CustomThemeColor.toHueTone(hex)
            assertNotNull(hueTone, hex)
            val (h, t) = hueTone!!
            assertTrue(h.isFinite() && h in 0f..360f, "$hex -> hue=$h")
            assertTrue(t.isFinite() && t in 0f..1f, "$hex -> tone=$t")
        }
    }

    @Test
    fun eightDigitHexIsSameAsSixDigit() {
        assertTrue(CustomThemeColor.isSameColor("#FF3FFFBA", "#3FFFBA"))
        assertFalse(CustomThemeColor.isSameColor("#3FFFBA", "#3FF3FF"))
        assertFalse(CustomThemeColor.isSameColor("invalid", "#3FFFBA"))
        assertNull(CustomThemeColor.toHueTone("invalid"))
    }

    private fun nearlySame(
        a: String,
        b: String,
    ): Boolean {
        val x = a.removePrefix("#").toInt(16)
        val y = b.removePrefix("#").toInt(16)
        return (0..2).all { i -> kotlin.math.abs(((x shr (i * 8)) and 0xFF) - ((y shr (i * 8)) and 0xFF)) <= 2 }
    }
}
