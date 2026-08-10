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

package com.nexters.bandalart.core.domain.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BandalartWidgetLaunchTargetTest {
    @Test
    fun buffersLatestValidTargetUntilMatchingAcknowledgement() {
        val target = BufferedBandalartWidgetLaunchTarget()

        target.record(0L)
        assertNull(target.pendingBandalartId.value)

        target.record(10L)
        target.record(20L)
        target.acknowledge(10L)
        assertEquals(20L, target.pendingBandalartId.value)

        target.acknowledge(20L)
        assertNull(target.pendingBandalartId.value)
    }
}
