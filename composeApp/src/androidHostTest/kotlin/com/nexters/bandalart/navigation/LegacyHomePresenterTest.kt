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

package com.nexters.bandalart.navigation

import com.nexters.bandalart.core.navigation.LegacyHomeScreen
import com.nexters.bandalart.feature.complete.CompleteScreen
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LegacyHomePresenterTest {
    @Test
    fun completeEventOpensCircuitCompleteScreen() =
        runTest {
            val navigator = FakeNavigator(LegacyHomeScreen)
            val presenter = LegacyHomePresenter(navigator)
            val event =
                LegacyHomeScreen.Event.NavigateToComplete(
                    bandalartId = 42L,
                    bandalartTitle = "출시 준비",
                    bandalartProfileEmoji = "🚀",
                    bandalartChartImageUri = "content://bandalart/chart",
                )

            presenter.test {
                awaitItem().eventSink(event)

                assertEquals(
                    CompleteScreen(
                        bandalartId = event.bandalartId,
                        bandalartTitle = event.bandalartTitle,
                        bandalartProfileEmoji = event.bandalartProfileEmoji,
                        bandalartChartImageUri = event.bandalartChartImageUri,
                    ),
                    navigator.awaitNextScreen(),
                )
            }
        }
}
