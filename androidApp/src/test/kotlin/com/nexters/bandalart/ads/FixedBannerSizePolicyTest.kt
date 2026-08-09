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

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FixedBannerSizePolicyTest {
    @Test
    fun widthBelow320DpDoesNotRenderBanner() {
        assertFalse(supportsFixedBanner(319.99f))
    }

    @Test
    fun widthAtLeast320DpRendersBanner() {
        assertTrue(supportsFixedBanner(320f))
        assertTrue(supportsFixedBanner(360f))
    }
}
