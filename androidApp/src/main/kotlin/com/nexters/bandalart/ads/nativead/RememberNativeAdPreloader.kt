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

package com.nexters.bandalart.ads.nativead

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.nexters.bandalart.ads.nonPersonalizedAdExtras
import io.github.aakira.napier.Napier
import kotlin.time.Duration.Companion.hours

@Composable
internal fun rememberNativeAdPreloader(
    adUnitId: String,
    awaitAdsInitialized: suspend () -> Boolean,
    preload: Boolean,
): NativeAdPreloader {
    val state =
        remember(adUnitId) {
            NativeAdState(
                ttlMillis = 1.hours.inWholeMilliseconds,
                elapsedRealtime = SystemClock::elapsedRealtime,
            )
        }
    val previewPreloader =
        remember(state) {
            NativeAdPreloader(
                state = state,
                requestAd = {},
                dispatchCallback = {},
                onAdFailedToLoad = {},
            )
        }
    if (LocalInspectionMode.current) return previewPreloader

    var isAdsInitialized by remember(adUnitId) { mutableStateOf(false) }
    LaunchedEffect(adUnitId, preload) {
        if (preload && adUnitId.isNotBlank() && !isAdsInitialized) {
            isAdsInitialized = awaitAdsInitialized()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val preloader =
        remember(state, lifecycleOwner, adUnitId) {
            val handler = Handler(Looper.getMainLooper())
            NativeAdPreloader(
                state = state,
                requestAd = { callback ->
                    NativeAdLoader.load(
                        NativeAdRequest
                            .Builder(adUnitId, listOf(NativeAd.NativeAdType.NATIVE))
                            .setGoogleExtrasBundle(nonPersonalizedAdExtras())
                            .build(),
                        callback,
                    )
                },
                dispatchCallback = { callback -> handler.post(callback) },
                onAdFailedToLoad = { error ->
                    Napier.w(
                        "Exit dialog native ad failed to load: $error",
                        tag = "NativeAd",
                    )
                },
            )
        }

    LifecycleResumeEffect(preloader, preload, isAdsInitialized) {
        if (isAdsInitialized && preload) preloader.enableLoading()
        onPauseOrDispose {}
    }

    DisposableEffect(preloader, lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) preloader.destroy()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            preloader.destroy()
        }
    }

    return preloader
}
