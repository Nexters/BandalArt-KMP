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

package com.nexters.bandalart.core.domain.notification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DeadlineNotificationLaunchTargetTest {
    @Test
    fun latestValidTargetIsBufferedUntilMatchingAcknowledgement() {
        val target = BufferedDeadlineNotificationLaunchTarget()

        target.record(-1)
        assertNull(target.pendingBandalartId.value)

        target.record(10)
        target.record(20)
        target.acknowledge(10)
        assertEquals(20L, target.pendingBandalartId.value)

        target.acknowledge(20)
        assertNull(target.pendingBandalartId.value)
    }
}
