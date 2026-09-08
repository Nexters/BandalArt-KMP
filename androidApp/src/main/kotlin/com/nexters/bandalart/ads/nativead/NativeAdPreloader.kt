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
    private val elapsedRealtime: () -> Long,
    private val minimumRequestIntervalMillis: Long,
    private val debugLog: (String) -> Unit = {},
) {
    private var destroyed = false
    private var loadingEnabled = false
    private var loading = false
    private var generation = 0L
    private var lastRequestAtMillis: Long? = null

    val ad: NativeAd?
        get() = state.ad

    fun adSnapshotForDisplay(): NativeAd? = state.ad?.also { debugLog("display instance=${it.identity()}") }

    fun enableLoading() {
        if (destroyed) return
        loadingEnabled = true
        loadIfNeeded()
    }

    fun loadIfNeeded() {
        if (destroyed || !loadingEnabled || loading) return
        if (state.isExpired()) {
            state.ad?.let { debugLog("expire instance=${it.identity()}") }
            state.clear()
        }
        if (state.ad != null) return

        val now = elapsedRealtime()
        val lastRequestAt = lastRequestAtMillis
        if (lastRequestAt != null && now - lastRequestAt < minimumRequestIntervalMillis) return

        loading = true
        val requestGeneration = ++generation
        lastRequestAtMillis = now
        debugLog("request generation=$requestGeneration")
        requestAd(
            object : NativeAdLoaderCallback {
                private var completed = false
                private var returnedAd: NativeAd? = null

                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    dispatchCallback {
                        if (returnedAd === nativeAd) return@dispatchCallback
                        returnedAd = nativeAd
                        if (completed || destroyed || requestGeneration != generation) {
                            debugLog(
                                "discard late generation=$requestGeneration instance=${nativeAd.identity()}",
                            )
                            nativeAd.destroy()
                            return@dispatchCallback
                        }
                        completed = true
                        loading = false
                        state.store(nativeAd)
                        debugLog("loaded generation=$requestGeneration instance=${nativeAd.identity()}")
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    dispatchCallback {
                        if (completed || destroyed || requestGeneration != generation) return@dispatchCallback
                        completed = true
                        loading = false
                        debugLog("failed generation=$requestGeneration")
                        onAdFailedToLoad(adError)
                    }
                }
            },
        )
    }

    fun disableLoading() {
        if (destroyed) return
        loadingEnabled = false
    }

    fun recycleIfEligible() {
        val nativeAd = state.ad ?: return
        val lastRequestAt = lastRequestAtMillis ?: return
        val requestAgeMillis = (elapsedRealtime() - lastRequestAt).coerceAtLeast(0)
        if (destroyed || requestAgeMillis < minimumRequestIntervalMillis) {
            debugLog("retain ageMs=$requestAgeMillis instance=${nativeAd.identity()}")
            return
        }
        debugLog("recycle ageMs=$requestAgeMillis instance=${nativeAd.identity()}")
        state.clear()
        loadIfNeeded()
    }

    fun discard() {
        if (destroyed) return
        state.ad?.let { debugLog("discard instance=${it.identity()}") }
        state.clear()
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        generation++
        state.ad?.let { debugLog("destroy instance=${it.identity()}") }
        state.clear()
    }

    private fun NativeAd.identity(): Int = System.identityHashCode(this)
}
