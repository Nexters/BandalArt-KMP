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

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BandalartWidgetProcessLifecycleObserverTest {
    @Test
    fun `refreshes widgets when the app process moves to background`() {
        var refreshCount = 0
        val observer = BandalartWidgetProcessLifecycleObserver(onBackground = { refreshCount += 1 })

        observer.onPause(UnusedLifecycleOwner)
        assertEquals(0, refreshCount)

        observer.onStop(UnusedLifecycleOwner)

        assertEquals(1, refreshCount)
    }

    private object UnusedLifecycleOwner : LifecycleOwner {
        override val lifecycle: Lifecycle
            get() = error("The widget lifecycle observer does not read owner state")
    }
}
