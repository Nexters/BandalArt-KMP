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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NativeAdRegistrationTest {
    @Test
    fun nativeAdIsRegisteredOnlyAfterItsMediaViewIsReady() {
        val registrations = mutableListOf<Triple<String, String, String>>()

        registerNativeAdWhenReady(
            nativeAdView = "ad-view",
            nativeAd = "ad",
            mediaView = null,
            register = { adView, ad, media -> registrations += Triple(adView, ad, media) },
        )
        registerNativeAdWhenReady(
            nativeAdView = "ad-view",
            nativeAd = "ad",
            mediaView = "media-view",
            register = { adView, ad, media -> registrations += Triple(adView, ad, media) },
        )

        assertEquals(
            listOf(Triple("ad-view", "ad", "media-view")),
            registrations,
        )
    }
}
