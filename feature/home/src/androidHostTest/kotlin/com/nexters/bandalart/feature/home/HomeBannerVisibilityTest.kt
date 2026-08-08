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

package com.nexters.bandalart.feature.home

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HomeBannerVisibilityTest {
    private val visibleState =
        HomeScreen.State(
            isLoading = false,
            eventSink = {},
        )

    @Test
    fun bannerIsVisibleOnIdleHome() {
        assertTrue(visibleState.isBannerCreativeVisible())
    }

    @Test
    fun bannerIsHiddenWhileLoading() {
        assertFalse(visibleState.copy(isLoading = true).isBannerCreativeVisible())
    }

    @Test
    fun bannerIsHiddenBehindBottomSheet() {
        assertFalse(
            visibleState
                .copy(bottomSheet = HomeScreen.BottomSheetState.Settings)
                .isBannerCreativeVisible(),
        )
    }

    @Test
    fun bannerIsHiddenBehindDialog() {
        assertFalse(
            visibleState
                .copy(dialog = HomeScreen.DialogState.BandalartDelete)
                .isBannerCreativeVisible(),
        )
    }

    @Test
    fun bannerIsHiddenDuringImageCapture() {
        assertFalse(
            visibleState
                .copy(imageRequest = HomeScreen.ImageRequest.Share)
                .isBannerCreativeVisible(),
        )
    }

    @Test
    fun bannerIsHiddenDuringRewardedAd() {
        assertFalse(
            visibleState
                .copy(rewardedAdRequestId = 42L)
                .isBannerCreativeVisible(),
        )
    }
}
