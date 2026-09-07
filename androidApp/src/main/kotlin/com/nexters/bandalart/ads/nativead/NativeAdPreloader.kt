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

import androidx.compose.runtime.Stable
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback

@Stable
internal class NativeAdPreloader(
    private val state: NativeAdState,
    private val requestAd: (NativeAdLoaderCallback) -> Unit,
    private val dispatchCallback: (() -> Unit) -> Unit,
    private val onAdFailedToLoad: (LoadAdError) -> Unit,
) {
    private var destroyed = false
    private var loadingEnabled = false
    private var loading = false
    private var generation = 0L

    val ad: NativeAd?
        get() = state.ad

    fun enableLoading() {
        if (destroyed) return
        loadingEnabled = true
        loadIfNeeded()
    }

    fun loadIfNeeded() {
        if (destroyed || !loadingEnabled || loading) return
        if (state.isExpired()) state.clear()
        if (state.ad != null) return

        loading = true
        val requestGeneration = ++generation
        requestAd(
            object : NativeAdLoaderCallback {
                private var completed = false
                private var returnedAd: NativeAd? = null

                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    dispatchCallback {
                        if (returnedAd === nativeAd) return@dispatchCallback
                        returnedAd = nativeAd
                        if (completed || destroyed || requestGeneration != generation) {
                            nativeAd.destroy()
                            return@dispatchCallback
                        }
                        completed = true
                        loading = false
                        state.store(nativeAd)
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    dispatchCallback {
                        if (completed || destroyed || requestGeneration != generation) return@dispatchCallback
                        completed = true
                        loading = false
                        onAdFailedToLoad(adError)
                    }
                }
            },
        )
    }

    fun recycle() {
        if (destroyed) return
        state.clear()
        loadIfNeeded()
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        generation++
        state.clear()
    }
}
