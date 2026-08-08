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

import android.view.View
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.nexters.bandalart.R
import com.nexters.bandalart.core.common.BannerAdHost
import io.github.aakira.napier.Napier
import java.util.concurrent.atomic.AtomicBoolean

class AndroidBannerAdHost(
    private val awaitAdsInitialized: suspend () -> Boolean,
) : BannerAdHost {
    @Composable
    override fun Content(
        visible: Boolean,
        modifier: Modifier,
    ) {
        val activity = LocalActivity.current ?: return
        val isPreviewMode = LocalInspectionMode.current

        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            val width = maxWidth.value.toInt().coerceAtLeast(1)

            key(activity, width) {
                val adSize =
                    remember {
                        AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, width)
                    }
                val adView = remember { AdView(activity) }
                val released = remember { AtomicBoolean(false) }
                var isLoaded by remember { mutableStateOf(false) }

                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(adSize.height.dp),
                    factory = { adView },
                    update = { view ->
                        view.visibility =
                            if (visible && isLoaded) {
                                View.VISIBLE
                            } else {
                                View.INVISIBLE
                            }
                    },
                    onRelease = { view ->
                        released.set(true)
                        view.destroy()
                    },
                )

                LaunchedEffect(adView, isPreviewMode) {
                    if (isPreviewMode || !awaitAdsInitialized()) return@LaunchedEffect

                    adView.loadAd(
                        BannerAdRequest
                            .Builder(activity.getString(R.string.admob_banner_ad_unit_id), adSize)
                            .build(),
                        object : AdLoadCallback<BannerAd> {
                            override fun onAdLoaded(ad: BannerAd) {
                                activity.runOnUiThread {
                                    if (released.get()) {
                                        ad.destroy()
                                    } else {
                                        isLoaded = true
                                    }
                                }
                                Napier.d("Banner ad loaded", tag = "BannerAd")
                            }

                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                activity.runOnUiThread {
                                    if (!released.get()) isLoaded = false
                                }
                                Napier.w("Banner ad failed to load: $adError", tag = "BannerAd")
                            }
                        },
                    )
                }
            }
        }
    }
}
