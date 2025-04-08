package com.nexters.bandalart.core.data.repository

import com.nexters.bandalart.core.datastore.BandalartDataStore
import com.nexters.bandalart.core.domain.repository.OnboardingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse

@DisplayName("OnboardingRepository 테스트")
class OnboardingRepositoryImplTest {

    // 테스트 대상
    private lateinit var onboardingRepository: OnboardingRepository

    // 의존성 모킹
    private lateinit var mockBandalartDataStore: BandalartDataStore

    @BeforeEach
    fun setUp() {
        mockBandalartDataStore = mockk<BandalartDataStore>()
        onboardingRepository = OnboardingRepositoryImpl(mockBandalartDataStore)
    }

    @Nested
    @DisplayName("온보딩 완료 상태")
    inner class OnboardingCompletedStatusTest {

        @Test
        @DisplayName("온보딩 완료 상태를 설정할 수 있어야 한다")
        fun testSetOnboardingCompletedStatus() = runTest {
            // given
            coEvery { mockBandalartDataStore.setOnboardingCompletedStatus(any()) } returns Unit

            // when
            onboardingRepository.setOnboardingCompletedStatus(true)

            // then
            coVerify { mockBandalartDataStore.setOnboardingCompletedStatus(true) }
        }

        @Test
        @DisplayName("온보딩 완료 상태를 가져올 수 있어야 한다 - 완료된 경우")
        fun testGetOnboardingCompletedStatusTrue() = runTest {
            // given
            coEvery { mockBandalartDataStore.getOnboardingCompletedStatus() } returns true

            // when
            val result = onboardingRepository.getOnboardingCompletedStatus()

            // then
            assertTrue(result)
            coVerify { mockBandalartDataStore.getOnboardingCompletedStatus() }
        }

        @Test
        @DisplayName("온보딩 완료 상태를 가져올 수 있어야 한다 - 완료되지 않은 경우")
        fun testGetOnboardingCompletedStatusFalse() = runTest {
            // given
            coEvery { mockBandalartDataStore.getOnboardingCompletedStatus() } returns false

            // when
            val result = onboardingRepository.getOnboardingCompletedStatus()

            // then
            assertFalse(result)
            coVerify { mockBandalartDataStore.getOnboardingCompletedStatus() }
        }
    }
}
