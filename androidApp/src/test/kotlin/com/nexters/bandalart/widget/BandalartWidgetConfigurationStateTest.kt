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

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class BandalartWidgetConfigurationStateTest {
    @Test
    fun `board and subgoal rows keep distinct keys when database ids overlap`() {
        assertNotEquals(
            bandalartConfigurationItemKey(bandalartId = 2L),
            subGoalConfigurationItemKey(subGoalId = 2L),
        )
    }

    @Test
    fun `saving configuration makes its board recent before widget rendering`() =
        runBlocking {
            val configuredSelection = BandalartWidgetSelection(bandalartId = 1L, subGoalId = 2L)
            var recentBandalartId = 3L
            var persistedSelection: BandalartWidgetSelection? = null

            saveWidgetConfiguration(
                selection = configuredSelection,
                setRecentBandalartId = { recentBandalartId = it },
                persistSelection = { persistedSelection = it },
            )

            assertEquals(
                configuredSelection,
                resolveWidgetSelection(
                    configuredSelection = persistedSelection,
                    recentBandalartId = recentBandalartId,
                ),
            )
        }
}
