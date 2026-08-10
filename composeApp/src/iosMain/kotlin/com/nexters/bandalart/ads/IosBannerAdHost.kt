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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import com.nexters.bandalart.core.common.BannerAdHost
import kotlinx.cinterop.ExperimentalForeignApi

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
            if (maxWidth.value < FIXED_BANNER_WIDTH_DP) return@BoxWithConstraints

            key(adsBridge) {
                UIKitView(
                    factory = adsBridge::makeBannerView,
                    modifier =
                        Modifier
                            .width(FIXED_BANNER_WIDTH_DP.dp)
                            .height(FIXED_BANNER_HEIGHT_DP.dp),
                    update = { view -> view.hidden = !visible },
                )
            }
        }
    }
}

private const val FIXED_BANNER_WIDTH_DP = 320
private const val FIXED_BANNER_HEIGHT_DP = 50
