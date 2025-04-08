package com.nexters.bandalart.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@DisplayName("BandalartDataStore 테스트")
class BandalartDataStoreTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var bandalartDataStore: BandalartDataStore

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        // 테스트용 임시 DataStore 생성
        dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempDir.resolve("test_datastore.preferences_pb") }
        )
        bandalartDataStore = BandalartDataStore(dataStore)
    }

    @Nested
    @DisplayName("최근 반다라트 ID 관련 테스트")
    inner class RecentBandalartIdTest {

        @Test
        @DisplayName("recentBandalartId를 설정하고 가져올 수 있어야 한다")
        fun testRecentBandalartId() = runTest {
            // given
            val testId = 123L

            // when
            bandalartDataStore.setRecentBandalartId(testId)
            val retrievedId = bandalartDataStore.getRecentBandalartId()

            // then
            assertEquals(testId, retrievedId)
        }

        @Test
        @DisplayName("값이 설정되지 않았을 때 기본값 0L을 반환해야 한다")
        fun testDefaultRecentBandalartId() = runTest {
            // when
            val retrievedId = bandalartDataStore.getRecentBandalartId()

            // then
            assertEquals(0L, retrievedId)
        }
    }

    @Nested
    @DisplayName("완료된 반다라트 목록 관련 테스트")
    inner class CompletedBandalartListTest {

        @Test
        @DisplayName("반다라트 ID를 추가하고 목록을 조회할 수 있어야 한다")
        fun testUpsertAndGetBandalartList() = runTest {
            // given
            val bandalartId1 = 1L
            val bandalartId2 = 2L

            // when
            bandalartDataStore.upsertBandalartId(bandalartId1, true)
            bandalartDataStore.upsertBandalartId(bandalartId2, false)
            val bandalartList = bandalartDataStore.getPrevBandalartList()

            // then
            assertEquals(2, bandalartList.size)
            assertTrue(bandalartList.any { it.first == bandalartId1 && it.second })
            assertTrue(bandalartList.any { it.first == bandalartId2 && !it.second })
        }

        @Test
        @DisplayName("이미 존재하는 ID의 경우 상태를 업데이트해야 한다")
        fun testUpdateExistingBandalartId() = runTest {
            // given
            val bandalartId = 5L

            // when: 처음엔 false로 추가
            bandalartDataStore.upsertBandalartId(bandalartId, false)
            // 그 후 true로 업데이트
            bandalartDataStore.upsertBandalartId(bandalartId, true)
            val bandalartList = bandalartDataStore.getPrevBandalartList()

            // then
            assertEquals(1, bandalartList.size)
            assertTrue(bandalartList.first().second) // true로 업데이트 되었어야 함
        }

        @Test
        @DisplayName("ID가 완료 상태로 변경되었는지 확인할 수 있어야 한다")
        fun testCheckCompletedBandalartId() = runTest {
            // given
            val bandalartId = 10L

            // when: 처음에는 완료되지 않은 상태로 저장
            bandalartDataStore.upsertBandalartId(bandalartId, false)

            // then: 처음 상태가 false이므로 "새롭게 완료됨"으로 판단되어 true 반환
            assertTrue(bandalartDataStore.checkCompletedBandalartId(bandalartId))

            // when: 상태를 완료로 업데이트
            bandalartDataStore.upsertBandalartId(bandalartId, true)

            // then: 이미 완료 상태였으므로 "새롭게 완료됨"이 아니므로 false 반환
            assertFalse(bandalartDataStore.checkCompletedBandalartId(bandalartId))
        }

        @Test
        @DisplayName("반다라트 ID를 삭제할 수 있어야 한다")
        fun testDeleteBandalartId() = runTest {
            // given
            val bandalartId1 = 1L
            val bandalartId2 = 2L
            bandalartDataStore.upsertBandalartId(bandalartId1, true)
            bandalartDataStore.upsertBandalartId(bandalartId2, false)

            // when
            bandalartDataStore.deleteBandalartId(bandalartId1)
            val bandalartList = bandalartDataStore.getPrevBandalartList()

            // then
            assertEquals(1, bandalartList.size)
            assertEquals(bandalartId2, bandalartList.first().first)
        }
    }

    @Nested
    @DisplayName("온보딩 완료 상태 관련 테스트")
    inner class OnboardingCompletedTest {

        @Test
        @DisplayName("온보딩 완료 상태를 설정하고 가져올 수 있어야 한다")
        fun testOnboardingCompletedStatus() = runTest {
            // when: 초기 상태 확인
            val initialStatus = bandalartDataStore.getOnboardingCompletedStatus()

            // then: 초기값은 false여야 함
            assertFalse(initialStatus)

            // when: 완료 상태로 설정
            bandalartDataStore.setOnboardingCompletedStatus(true)
            val updatedStatus = bandalartDataStore.getOnboardingCompletedStatus()

            // then: 업데이트된 상태는 true여야 함
            assertTrue(updatedStatus)
        }
    }
}
