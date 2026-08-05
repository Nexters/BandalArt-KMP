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

package com.nexters.bandalart.feature.splash.presenter

import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.onboarding.OnboardingScreen
import com.nexters.bandalart.feature.splash.SplashScreen
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SplashPresenterTest {
    @Test
    fun completedOnboardingOpensHome() =
        runTest {
            assertDestination(isCompleted = true, expected = HomeScreen)
        }

    @Test
    fun incompleteOnboardingOpensOnboardingWithoutCreatingData() =
        runTest {
            assertDestination(isCompleted = false, expected = OnboardingScreen)
        }

    @Test
    fun duplicateCheckEventIsHandledOnce() =
        runTest {
            val repository = FakeOnboardingRepository(initialCompletedStatus = true)
            val navigator = FakeNavigator(SplashScreen)
            val presenter = SplashPresenter(navigator, repository)

            presenter.test {
                val state = awaitItem()

                state.eventSink(SplashScreen.Event.CheckOnboardingStatus)
                state.eventSink(SplashScreen.Event.CheckOnboardingStatus)

                assertEquals(HomeScreen, navigator.awaitResetRoot().newRoot)
                assertEquals(1, repository.getCallCount)
                navigator.expectNoResetRootEvents()
            }
        }

    private suspend fun assertDestination(
        isCompleted: Boolean,
        expected: Screen,
    ) {
        val repository = FakeOnboardingRepository(isCompleted)
        val navigator = FakeNavigator(SplashScreen)
        val presenter = SplashPresenter(navigator, repository)

        presenter.test {
            awaitItem().eventSink(SplashScreen.Event.CheckOnboardingStatus)

            assertEquals(expected, navigator.awaitResetRoot().newRoot)
            assertEquals(1, repository.getCallCount)
        }
    }
}
