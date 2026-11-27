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
        for (hue in 0..360) {
            for (tone in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                val color = CustomThemeColor.toThemeColor(hue.toFloat(), tone)
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
