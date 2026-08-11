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

package com.nexters.bandalart.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BandalartWidgetLaunchRequestTest {
    @Test
    fun `accepts a positive bandalart id for the widget action`() {
        val request =
            BandalartWidgetLaunchRequest.from(
                action = BandalartWidgetLaunchRequest.ACTION_OPEN_BANDALART,
                bandalartId = 7L,
            )

        assertEquals(BandalartWidgetLaunchRequest(7L), request)
    }

    @Test
    fun `rejects another action or invalid id`() {
        assertNull(BandalartWidgetLaunchRequest.from(action = "other", bandalartId = 7L))
        assertNull(
            BandalartWidgetLaunchRequest.from(
                action = BandalartWidgetLaunchRequest.ACTION_OPEN_BANDALART,
                bandalartId = 0L,
            ),
        )
    }
}
