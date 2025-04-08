package com.nexters.bandalart.core.data.repository

import com.nexters.bandalart.core.datastore.InAppUpdateDataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

object VersionTestData {
    @JvmStatic
    fun versionProvider() = listOf(
        Arguments.of(1, 0, 0, 10000),   // 1.0.0
        Arguments.of(2, 2, 0, 20200),   // 2.2.0
        Arguments.of(2, 2, 5, 20205),   // 2.2.5
        Arguments.of(3, 1, 2, 30102),   // 3.1.2
        Arguments.of(10, 5, 3, 100503)  // 10.5.3
    )
}

@DisplayName("InAppUpdateRepository 테스트")
class InAppUpdateRepositoryImplTest {

    // 테스트 대상
    private lateinit var inAppUpdateRepository: DefaultInAppUpdateRepository

    // 의존성 모킹
    private lateinit var mockInAppUpdateDataStore: InAppUpdateDataStore

    @BeforeEach
    fun setUp() {
        mockInAppUpdateDataStore = mockk<InAppUpdateDataStore>()
        inAppUpdateRepository = DefaultInAppUpdateRepository(mockInAppUpdateDataStore)
    }

    @Nested
    @DisplayName("앱 내 업데이트 거절 버전 관리")
    inner class InAppUpdateRejectionTest {

        @Test
        @DisplayName("거절된 업데이트 버전을 설정할 수 있어야 한다")
        fun testSetLastRejectedUpdateVersion() = runTest {
            // given
            val rejectedVersionCode = calculateVersionCode(2, 2, 0)
            coEvery { mockInAppUpdateDataStore.setLastRejectedUpdateVersion(any()) } returns Unit

            // when
            inAppUpdateRepository.setLastRejectedUpdateVersion(rejectedVersionCode)

            // then
            coVerify { mockInAppUpdateDataStore.setLastRejectedUpdateVersion(rejectedVersionCode) }
        }

        @ParameterizedTest(name = "버전 {0}.{1}.{2}의 거부 코드 {3}에 대해 이미 거절된 업데이트인지 확인")
        @MethodSource("com.nexters.bandalart.core.data.repository.VersionTestData#versionProvider")
        @DisplayName("다양한 버전 코드에 대해 업데이트 거절 상태를 정확히 판단해야 한다")
        fun testIsUpdateAlreadyRejected(
            major: Int,
            minor: Int,
            patch: Int,
            currentVersionCode: Int
        ) = runTest {
            // scenario 1: 현재 버전이 거절된 버전과 동일한 경우
            val sameVersionCode = currentVersionCode
            coEvery { mockInAppUpdateDataStore.getLastRejectedUpdateVersion() } returns sameVersionCode
            assertTrue(inAppUpdateRepository.isUpdateAlreadyRejected(sameVersionCode))

            // scenario 2: 현재 버전이 거절된 버전보다 낮은 경우
            val lowerVersionCode = currentVersionCode - 1
            coEvery { mockInAppUpdateDataStore.getLastRejectedUpdateVersion() } returns currentVersionCode
            assertTrue(inAppUpdateRepository.isUpdateAlreadyRejected(lowerVersionCode))

            // scenario 3: 현재 버전이 거절된 버전보다 높은 경우
            val higherVersionCode = currentVersionCode + 1
            coEvery { mockInAppUpdateDataStore.getLastRejectedUpdateVersion() } returns currentVersionCode
            assertFalse(inAppUpdateRepository.isUpdateAlreadyRejected(higherVersionCode))
        }
    }

    // 버전 코드 계산 함수 (DataStore 테스트와 동일한 로직)
    private fun calculateVersionCode(major: Int, minor: Int, patch: Int): Int {
        return (major * 10000) + (minor * 100) + patch
    }
}
