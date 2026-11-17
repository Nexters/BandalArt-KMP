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

package com.nexters.bandalart.feature.onboarding.presenter

import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.onboarding.OnboardingRecordIntroScreen
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OnboardingRecordIntroPresenterTest {
    @Test
    fun confirmMarksIntroShownBeforeOpeningHomeWithoutTouchingOnboardingStatus() =
        runTest {
            val repository = FakeOnboardingRepository()
            val navigator = FakeNavigator(OnboardingRecordIntroScreen)

            OnboardingRecordIntroPresenter(navigator, repository).test {
                val state = awaitItem()
                state.eventSink(OnboardingRecordIntroScreen.Event.Confirm)
                state.eventSink(OnboardingRecordIntroScreen.Event.Confirm)

                assertEquals(HomeScreen, navigator.awaitResetRoot().newRoot)
                assertTrue(repository.isRecordIntroShown)
                assertEquals(1, repository.recordIntroMarkCount)
                assertFalse(repository.isCompleted)
                assertEquals(0, repository.setCallCount)
                navigator.expectNoResetRootEvents()
            }
        }
}
