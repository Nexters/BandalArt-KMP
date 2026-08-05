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

package com.nexters.bandalart.feature.complete.presenter

import com.eygraber.uri.Uri
import com.nexters.bandalart.feature.complete.CompleteScreen
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CompletePresenterTest {
    private val screen =
        CompleteScreen(
            bandalartId = 42L,
            bandalartTitle = "출시 준비",
            bandalartProfileEmoji = "🚀",
            bandalartChartImageUri = "content://bandalart/chart",
        )

    @Test
    fun screenDataIsExposedAndBandalartIsMarkedComplete() =
        runTest {
            val repository = RecordingBandalartRepository()
            val presenter = CompletePresenter(FakeNavigator(screen), screen, repository)

            presenter.test {
                val state = awaitItem()

                assertEquals(screen.bandalartId, state.id)
                assertEquals(screen.bandalartTitle, state.title)
                assertEquals(screen.bandalartProfileEmoji, state.profileEmoji)
                assertEquals(screen.bandalartChartImageUri, state.bandalartChartImageUri)

                repository.awaitCompletion()
                assertEquals(screen.bandalartId to true, repository.completion)
                assertEquals(1, repository.completionCount)
            }
        }

    @Test
    fun saveAndShareEventsAreExposedAsSideEffects() =
        runTest {
            val presenter =
                CompletePresenter(
                    navigator = FakeNavigator(screen),
                    screen = screen,
                    bandalartRepository = RecordingBandalartRepository(),
                )
            val imageUri = Uri.parse(screen.bandalartChartImageUri)

            presenter.test {
                var state = awaitItem()

                state.eventSink(CompleteScreen.Event.SaveBandalart(imageUri))
                state = awaitItem()
                assertEquals(CompleteScreen.SideEffect.SaveImage(imageUri), state.sideEffect)

                state.eventSink(CompleteScreen.Event.ShareBandalart(imageUri))
                state = awaitItem()
                assertEquals(CompleteScreen.SideEffect.ShareImage(imageUri), state.sideEffect)

                state.eventSink(CompleteScreen.Event.ClearSideEffect)
                assertNull(awaitItem().sideEffect)
            }
        }

    @Test
    fun navigateBackPopsCurrentScreen() =
        runTest {
            val rootScreen = screen.copy(bandalartId = 0L)
            val navigator = FakeNavigator(rootScreen, screen)
            val presenter = CompletePresenter(navigator, screen, RecordingBandalartRepository())

            presenter.test {
                awaitItem().eventSink(CompleteScreen.Event.NavigateBack)

                assertEquals(screen, navigator.awaitPop().poppedScreen)
            }
        }
}
