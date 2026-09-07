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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd

@Stable
internal class NativeAdState(
    ttlMillis: Long,
    elapsedRealtime: () -> Long,
) {
    private var loadedAtMillis: Long = 0

    var ad: NativeAd? by mutableStateOf(null)
        private set

    private val ttlMillis = ttlMillis
    private val elapsedRealtime = elapsedRealtime

    fun store(newAd: NativeAd) {
        ad?.destroy()
        ad = newAd
        loadedAtMillis = elapsedRealtime()
    }

    fun clear() {
        ad?.destroy()
        ad = null
    }

    fun remainingMillis(): Long {
        if (ad == null) return 0
        return (loadedAtMillis + ttlMillis - elapsedRealtime()).coerceAtLeast(0)
    }

    fun isExpired(): Boolean = ad != null && remainingMillis() == 0L
}
