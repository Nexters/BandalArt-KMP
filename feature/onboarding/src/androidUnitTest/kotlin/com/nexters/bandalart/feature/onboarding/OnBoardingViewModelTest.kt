/*
 * Copyright 2025 easyhooon
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

package com.nexters.bandalart.feature.onboarding

import app.cash.turbine.test
import com.nexters.bandalart.core.domain.repository.OnboardingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("OnboardingViewModel 테스트")
class OnboardingViewModelTest {
    private lateinit var onboardingViewModel: OnboardingViewModel
    private lateinit var mockOnboardingRepository: OnboardingRepository
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockOnboardingRepository = mockk()
        onboardingViewModel = OnboardingViewModel(mockOnboardingRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("온보딩 완료 상태 설정 후 홈 화면으로 이동해야 한다")
    fun testSetOnboardingCompletedStatusNavigatesToHome() =
        runTest {
            // given
            val completedFlag = true
            coEvery { mockOnboardingRepository.setOnboardingCompletedStatus(completedFlag) } returns Unit

            // when
            onboardingViewModel.setOnboardingCompletedStatus(completedFlag)

            // then
            onboardingViewModel.uiEvent.test {
                testScheduler.advanceUntilIdle()

                assertEquals(OnBoardingUiEvent.NavigateToHome, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            // verify
            coVerify { mockOnboardingRepository.setOnboardingCompletedStatus(completedFlag) }
        }
}
