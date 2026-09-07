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

import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class NativeAdPreloaderTest {
    private class Fixture(
        private val dispatch: (() -> Unit) -> Unit = { it() },
    ) {
        var elapsedRealtime = 0L
        val requests = mutableListOf<NativeAdLoaderCallback>()
        val state = NativeAdState(ttlMillis = 3_600_000L) { elapsedRealtime }
        val preloader =
            NativeAdPreloader(
                state = state,
                requestAd = requests::add,
                dispatchCallback = dispatch,
                onAdFailedToLoad = {},
            )

        fun succeed(ad: NativeAd = mockk(relaxed = true)): NativeAd {
            requests.last().onNativeAdLoaded(ad)
            return ad
        }
    }

    @Test
    fun repeatedPreloadKeepsOnlyOneRequestAndCachedAd() {
        val fixture = Fixture()

        fixture.preloader.enableLoading()
        fixture.preloader.loadIfNeeded()
        val ad = fixture.succeed()
        fixture.preloader.enableLoading()

        assertEquals(1, fixture.requests.size)
        assertSame(ad, fixture.preloader.ad)
    }

    @Test
    fun recyclingDisplayedAdDestroysItAndPreloadsTheNextOne() {
        val fixture = Fixture()
        fixture.preloader.enableLoading()
        val ad = fixture.succeed()

        fixture.preloader.recycle()

        verify(exactly = 1) { ad.destroy() }
        assertNull(fixture.preloader.ad)
        assertEquals(2, fixture.requests.size)
    }

    @Test
    fun expiredCachedAdIsReplacedWhenItIsNextNeeded() {
        val fixture = Fixture()
        fixture.preloader.enableLoading()
        val ad = fixture.succeed()
        fixture.elapsedRealtime = 3_600_000L

        fixture.preloader.loadIfNeeded()

        verify(exactly = 1) { ad.destroy() }
        assertNull(fixture.preloader.ad)
        assertEquals(2, fixture.requests.size)
    }

    @Test
    fun delayedAdAfterDestroyIsReleasedInsteadOfStored() {
        val pendingCallbacks = mutableListOf<() -> Unit>()
        val fixture = Fixture(dispatch = pendingCallbacks::add)
        val ad = mockk<NativeAd>(relaxed = true)
        fixture.preloader.enableLoading()
        fixture.requests.single().onNativeAdLoaded(ad)

        fixture.preloader.destroy()
        pendingCallbacks.single().invoke()

        assertNull(fixture.preloader.ad)
        verify(exactly = 1) { ad.destroy() }
    }
}
