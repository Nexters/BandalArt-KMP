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

package com.nexters.bandalart.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import com.nexters.bandalart.core.common.BannerAdHost
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIColor
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

class IosBannerAdHost(
    private val adsBridge: IosAdsBridge,
) : BannerAdHost {
    @OptIn(ExperimentalForeignApi::class)
    @Composable
    override fun Content(
        visible: Boolean,
        modifier: Modifier,
    ) {
        BoxWithConstraints(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            if (maxWidth.value < FIXED_BANNER_WIDTH_DP || isScreenshotMode) return@BoxWithConstraints

            // 광고를 받기 전이나 받지 못했을 때 interop 자리 뒤의 흰 배경이 비치지 않도록 화면 배경색을 칠해 둔다.
            val backgroundColor = MaterialTheme.colorScheme.background
            key(adsBridge) {
                UIKitView(
                    factory = adsBridge::makeBannerView,
                    modifier =
                        Modifier
                            .width(FIXED_BANNER_WIDTH_DP.dp)
                            .height(FIXED_BANNER_HEIGHT_DP.dp),
                    update = { view ->
                        view.hidden = !visible
                        view.backgroundColor =
                            UIColor.colorWithRed(
                                red = backgroundColor.red.toDouble(),
                                green = backgroundColor.green.toDouble(),
                                blue = backgroundColor.blue.toDouble(),
                                alpha = backgroundColor.alpha.toDouble(),
                            )
                    },
                )
            }
        }
    }
}

// 스토어 스크린샷 촬영용: Debug 빌드를 `simctl launch ... -hideAdsForScreenshots`로 실행하면 광고 칸을 그리지 않는다.
// 배너만 숨기면 interop 자리가 흰 사각형으로 남기 때문에 UIKitView 자체를 만들지 않는다.
@OptIn(ExperimentalNativeApi::class)
private val isScreenshotMode =
    Platform.isDebugBinary && NSProcessInfo.processInfo.arguments.contains("-hideAdsForScreenshots")

private const val FIXED_BANNER_WIDTH_DP = 320
private const val FIXED_BANNER_HEIGHT_DP = 50
