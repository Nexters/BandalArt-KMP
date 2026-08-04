package com.nexters.bandalart.feature.splash

import app.cash.turbine.test
import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.core.domain.repository.BandalartRepository
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
@DisplayName("SplashViewModel 테스트")
class SplashViewModelTest {

    private lateinit var splashViewModel: SplashViewModel
    private lateinit var mockOnboardingRepository: OnboardingRepository
    private lateinit var mockBandalartRepository: BandalartRepository
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockOnboardingRepository = mockk()
        mockBandalartRepository = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("온보딩이 완료된 경우 홈 화면으로 이동해야 한다")
    fun testOnboardingCompletedNavigatesToHome() = runTest {
        // given
        coEvery { mockOnboardingRepository.getOnboardingCompletedStatus() } returns true

        // ViewModel 초기화
        val splashViewModel = SplashViewModel(mockOnboardingRepository, mockBandalartRepository)

        // when & then
        splashViewModel.uiEvent.test {
            // 테스트 디스패처 실행
            testScheduler.advanceUntilIdle()

            // 이벤트 확인
            assertEquals(SplashUiEvent.NavigateToHome, awaitItem())

            // 더 이상의 이벤트를 기다리지 않고 테스트 종료
            cancelAndIgnoreRemainingEvents()
        }

        // verify
        coVerify { mockOnboardingRepository.getOnboardingCompletedStatus() }
        coVerify(exactly = 0) { mockBandalartRepository.createBandalart() }
    }

    @Test
    @DisplayName("온보딩이 완료되지 않은 경우 새 반다라트를 생성하고 온보딩 화면으로 이동해야 한다")
    fun testOnboardingNotCompletedCreatesNewBandalartAndNavigatesToOnboarding() = runTest {
        // given
        val mockBandalartEntity = mockk<BandalartEntity>()
        coEvery { mockOnboardingRepository.getOnboardingCompletedStatus() } returns false
        coEvery { mockBandalartRepository.createBandalart() } returns mockBandalartEntity

        // ViewModel 초기화
        splashViewModel = SplashViewModel(mockOnboardingRepository, mockBandalartRepository)

        // when & then
        splashViewModel.uiEvent.test {
            // 테스트 디스패처 실행
            testScheduler.advanceUntilIdle()

            // 이벤트 확인
            assertEquals(SplashUiEvent.NavigateToOnBoarding, awaitItem())

            // 더 이상의 이벤트를 기다리지 않고 테스트 종료
            cancelAndIgnoreRemainingEvents()
        }

        // verify
        coVerify { mockOnboardingRepository.getOnboardingCompletedStatus() }
        coVerify(exactly = 1) { mockBandalartRepository.createBandalart() }
    }
}
