/*
 * Copyright 2025 easyhooon
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

package com.nexters.bandalart.feature.home.ui.bandalart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.bottomsheet_color_custom
import bandalart.core.designsystem.generated.resources.bottomsheet_color_hue
import bandalart.core.designsystem.generated.resources.bottomsheet_color_lightness
import com.nexters.bandalart.core.common.extension.noRippleClickable
import com.nexters.bandalart.core.common.extension.toColor
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.ui.CustomThemeColor
import com.nexters.bandalart.core.ui.ThemeColor
import com.nexters.bandalart.core.ui.allColor
import org.jetbrains.compose.resources.stringResource

@Composable
fun BandalartColorPicker(
    initColor: ThemeColor,
    onColorSelect: (ThemeColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    var initSelected by remember { mutableStateOf(initColor) }
    val isCustomSelected = allColor.none { CustomThemeColor.isSameColor(it.mainColor, initSelected.mainColor) }
    val initialHueTone = remember { CustomThemeColor.toHueTone(initColor.mainColor) ?: (DEFAULT_HUE to DEFAULT_TONE) }
    var hue by rememberSaveable { mutableFloatStateOf(initialHueTone.first) }
    var tone by rememberSaveable { mutableFloatStateOf(initialHueTone.second) }
    var isCustomPanelOpened by rememberSaveable { mutableStateOf(false) }

    fun selectCustom() {
        initSelected = CustomThemeColor.toThemeColor(hue, tone)
        onColorSelect(initSelected)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        allColor.chunked(COLORS_PER_ROW).forEach { rowColors ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(45.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                rowColors.forEach {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (CustomThemeColor.isSameColor(it.mainColor, initSelected.mainColor)) {
                            SelectedRing()
                        }
                        Card(
                            modifier =
                                Modifier
                                    .height(36.dp)
                                    .aspectRatio(1f)
                                    .noRippleClickable {
                                        initSelected = it
                                        CustomThemeColor.toHueTone(it.mainColor)?.let { (presetHue, presetTone) ->
                                            hue = presetHue
                                            tone = presetTone
                                        }
                                        onColorSelect(initSelected)
                                    },
                            shape = RoundedCornerShape(90.dp),
                            colors = CardDefaults.cardColors(containerColor = it.mainColor.toColor()),
                        ) { }
                    }
                }
            }
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .noRippleClickable { isCustomPanelOpened = !isCustomPanelOpened },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 그리드 첫 칸과 같은 폭(1/6)에 원을 맞춘다.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cellWidth = maxWidth / COLORS_PER_ROW
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.width(cellWidth),
                    ) {
                        if (isCustomSelected) {
                            SelectedRing()
                        }
                        Box(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .background(
                                        brush =
                                            if (isCustomSelected) {
                                                SolidColor(initSelected.mainColor.toColor())
                                            } else {
                                                Brush.sweepGradient(hueStops())
                                            },
                                        shape = CircleShape,
                                    ),
                        )
                    }
                    Text(
                        text = stringResource(Res.string.bottomsheet_color_custom),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W600,
                    )
                }
            }
        }
        AnimatedVisibility(visible = isCustomPanelOpened) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ColorSlider(
                    label = stringResource(Res.string.bottomsheet_color_hue),
                    value = hue / MAX_HUE,
                    trackColors = hueStops(),
                    onValueChange = {
                        hue = it * MAX_HUE
                        selectCustom()
                    },
                )
                ColorSlider(
                    label = stringResource(Res.string.bottomsheet_color_lightness),
                    value = tone,
                    // OKLCH 밝기는 sRGB에서 선형이 아니라서 양 끝 두 색만으로는 트랙이 실제 결과와 어긋난다.
                    trackColors =
                        (0..LIGHTNESS_STOP_COUNT).map { i ->
                            CustomThemeColor.toThemeColor(hue, i.toFloat() / LIGHTNESS_STOP_COUNT).mainColor.toColor()
                        },
                    onValueChange = {
                        tone = it
                        selectCustom()
                    },
                )
            }
        }
    }
}

@Composable
private fun SelectedRing() {
    Card(
        border = BorderStroke(width = 1.5.dp, color = MaterialTheme.colorScheme.onSurface),
        modifier =
            Modifier
                .height(45.dp)
                .aspectRatio(1f),
        shape = RoundedCornerShape(90.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
    ) { }
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    trackColors: List<Color>,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.W600,
        )
        // 포인터 좌표와 그라데이션은 레이아웃 방향과 무관하게 왼쪽이 원점이므로, 썸 배치도 LTR로 고정한다.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            ColorSliderTrack(value = value, trackColors = trackColors, label = label, onValueChange = onValueChange)
        }
    }
}

@Composable
private fun ColorSliderTrack(
    value: Float,
    trackColors: List<Color>,
    label: String,
    onValueChange: (Float) -> Unit,
) {
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(SLIDER_THUMB_SIZE)
                .semantics {
                    contentDescription = label
                    progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                    setProgress { target ->
                        onValueChange(target.coerceIn(0f, 1f))
                        true
                    }
                },
        contentAlignment = Alignment.CenterStart,
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val thumbPx = with(LocalDensity.current) { SLIDER_THUMB_SIZE.toPx() }

        fun valueAt(x: Float): Float = ((x - thumbPx / 2) / (widthPx - thumbPx)).coerceIn(0f, 1f)

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(Brush.horizontalGradient(trackColors), RoundedCornerShape(6.dp)),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(SLIDER_THUMB_SIZE)
                    .pointerInput(widthPx) {
                        detectTapGestures { offset -> onValueChange(valueAt(offset.x)) }
                    }.pointerInput(widthPx) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            onValueChange(valueAt(change.position.x))
                        }
                    },
        )
        Spacer(
            modifier =
                Modifier
                    .offset(x = (maxWidth - SLIDER_THUMB_SIZE) * value)
                    .size(SLIDER_THUMB_SIZE)
                    .shadow(2.dp, CircleShape)
                    .background(Color.White, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
    }
}

private fun hueStops(): List<Color> =
    (0..HUE_STOP_COUNT).map { i ->
        CustomThemeColor.toThemeColor(MAX_HUE * i / HUE_STOP_COUNT, 0f).mainColor.toColor()
    }

private const val COLORS_PER_ROW = 6
private const val MAX_HUE = 360f
private const val HUE_STOP_COUNT = 12
private const val LIGHTNESS_STOP_COUNT = 4
private const val DEFAULT_HUE = 200f
private const val DEFAULT_TONE = 0.5f
private val SLIDER_THUMB_SIZE = 24.dp

// @ComponentPreview
@Preview
@Composable
private fun BandalartColorPickerPreview() {
    BandalartTheme {
        BandalartColorPicker(
            initColor = ThemeColor("#3FFFBA", "#111827"),
            onColorSelect = {},
        )
    }
}
