package com.nexters.bandalart.core.common.extension

import androidx.compose.ui.graphics.Color
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import android.graphics.Color as AndroidColor

@DisplayName("String 확장 함수 테스트")
class ColorExtensionsTest {

    @BeforeEach
    fun setUp() {
        // android.graphics.Color의 정적 메소드 모킹
        mockkStatic(AndroidColor::parseColor)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Nested
    @DisplayName("toColor 함수는")
    inner class ToColorTest {

        @ParameterizedTest
        @ValueSource(strings = ["#FF0000", "#00FF00", "#0000FF", "#FFFFFF", "#000000"])
        fun `유효한 색상 코드를 Compose Color 객체로 변환해야 한다`(colorString: String) {
            // given
            val expectedAndroidColor = when (colorString) {
                "#FF0000" -> 0xFFFF0000.toInt() // Red
                "#00FF00" -> 0xFF00FF00.toInt() // Green
                "#0000FF" -> 0xFF0000FF.toInt() // Blue
                "#FFFFFF" -> 0xFFFFFFFF.toInt() // White
                "#000000" -> 0xFF000000.toInt() // Black
                else -> 0
            }

            // AndroidColor.parseColor 메서드를 모킹
            every { AndroidColor.parseColor(colorString) } returns expectedAndroidColor

            // when
            val result = colorString.toColor()

            // then
            // Color 객체는 long 값을 갖고 있으므로 expectedAndroidColor를 long으로 변환하여 비교
            assertEquals(Color(expectedAndroidColor.toLong()), result)
        }

        @Test
        fun `유효하지 않은 색상 코드에 대한 예외를 던져야 한다`() {
            // given
            val invalidColorString = "invalid_color"

            // AndroidColor.parseColor가 예외를 던지도록 설정
            every { AndroidColor.parseColor(invalidColorString) } throws IllegalArgumentException("Unknown color")

            // when & then
            assertThrows<IllegalArgumentException> {
                invalidColorString.toColor()
            }
        }
    }
}
