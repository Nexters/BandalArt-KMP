/*
 * Copyright 2026 easyhooon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nexters.bandalart.feature.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.unit.dp
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.designsystem.theme.DarkBackground
import com.nexters.bandalart.core.designsystem.theme.Gray50
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Suppress("FunctionNaming")
class HomeCaptureBackgroundTest {
    @Test
    fun `dark theme share bitmap uses the screen background color`() =
        assertCaptureBackground(
            darkTheme = true,
            expectedBackground = DarkBackground,
        )

    @Test
    fun `light theme share bitmap keeps the screen background color`() =
        assertCaptureBackground(
            darkTheme = false,
            expectedBackground = Gray50,
        )

    private fun assertCaptureBackground(
        darkTheme: Boolean,
        expectedBackground: Color,
    ) {
        runAndroidComposeUiTest<ComponentActivity> {
            lateinit var homeGraphicsLayer: GraphicsLayer

            setContent {
                BandalartTheme(darkTheme = darkTheme) {
                    homeGraphicsLayer = rememberGraphicsLayer()
                    Box(
                        modifier =
                            Modifier
                                .size(20.dp)
                                .captureBandalartToGraphicsLayer(homeGraphicsLayer),
                    )
                }
            }

            waitForIdle()
            runOnUiThread {
                val root = requireNotNull(activity).window.decorView
                root.measure(
                    View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
                )
                root.layout(0, 0, 360, 800)
                root.draw(Canvas(Bitmap.createBitmap(360, 800, Bitmap.Config.ARGB_8888)))
            }

            val capturedBackground = homeGraphicsLayer.toImageBitmap().toPixelMap()[0, 0]
            assertEquals(expectedBackground, capturedBackground)
        }
    }
}
