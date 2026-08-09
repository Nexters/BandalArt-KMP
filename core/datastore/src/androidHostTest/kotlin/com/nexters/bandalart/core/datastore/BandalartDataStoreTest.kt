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

package com.nexters.bandalart.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
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
        dataStore =
            PreferenceDataStoreFactory.create(
                produceFile = { tempDir.resolve("test_datastore.preferences_pb") }
            )
        bandalartDataStore = BandalartDataStore(dataStore)
    }

    @Nested
    @DisplayName("마감일 알림 설정 테스트")
    inner class DeadlineReminderPreferenceTest {
        @Test
        @DisplayName("저장값이 없으면 마감일 알림은 꺼져 있어야 한다")
        fun deadlineReminderIsDisabledByDefault() =
            runTest {
                assertFalse(bandalartDataStore.deadlineReminderEnabled.first())
            }

        @Test
        @DisplayName("마감일 알림 사용 의사를 저장할 수 있어야 한다")
        fun deadlineReminderSelectionIsStored() =
            runTest {
                bandalartDataStore.setDeadlineReminderEnabled(true)

                assertTrue(bandalartDataStore.deadlineReminderEnabled.first())
            }
    }

    @Nested
    @DisplayName("최근 반다라트 ID 관련 테스트")
    inner class RecentBandalartIdTest {
        @Test
        @DisplayName("recentBandalartId를 설정하고 가져올 수 있어야 한다")
        fun testRecentBandalartId() =
            runTest {
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
        fun testDefaultRecentBandalartId() =
            runTest {
                // when
                val retrievedId = bandalartDataStore.getRecentBandalartId()

                // then
                assertEquals(0L, retrievedId)
            }
    }

    @Nested
    @DisplayName("반다라트 최대 슬롯 관련 테스트")
    inner class MaxBandalartSlotsTest {
        @Test
        @DisplayName("저장값이 없으면 전달된 최소 슬롯을 저장해야 한다")
        fun minimumSlotsAreStoredByDefault() =
            runTest {
                assertEquals(3, bandalartDataStore.resolveMaxBandalartSlots(minimumSlots = 3))
                assertEquals(3, bandalartDataStore.resolveMaxBandalartSlots(minimumSlots = 2))
            }

        @Test
        @DisplayName("기존 보유 개수가 저장값보다 크면 최대 슬롯을 보정해야 한다")
        fun currentCountRaisesStoredSlots() =
            runTest {
                bandalartDataStore.resolveMaxBandalartSlots(minimumSlots = 3)

                assertEquals(5, bandalartDataStore.resolveMaxBandalartSlots(minimumSlots = 5))
            }

        @Test
        @DisplayName("확장할 때 저장된 최대 슬롯을 정확히 하나 늘려야 한다")
        fun expansionIncrementsStoredSlots() =
            runTest {
                bandalartDataStore.resolveMaxBandalartSlots(minimumSlots = 5)

                assertEquals(6, bandalartDataStore.expandMaxBandalartSlots(minimumSlots = 3))
                assertEquals(6, bandalartDataStore.resolveMaxBandalartSlots(minimumSlots = 3))
            }

        @Test
        @DisplayName("보상 요청을 grant하면 목표 슬롯과 복구 상태를 원자적으로 저장해야 한다")
        fun rewardedGrantPersistsTargetAndRecoveryState() =
            runTest {
                bandalartDataStore.resolveMaxBandalartSlots(minimumSlots = 3)
                val pending =
                    bandalartDataStore.prepareRewardedCreation(
                        requestId = 42L,
                        minimumSlots = 3,
                        templateId = "study_plan_v1",
                    )

                assertEquals(StoredPendingRewardedCreation(42L, 4, false, "study_plan_v1"), pending)
                assertEquals(
                    StoredPendingRewardedCreation(42L, 4, true, "study_plan_v1"),
                    bandalartDataStore.grantRewardedCreation(42L),
                )
                assertEquals(4, bandalartDataStore.resolveMaxBandalartSlots(minimumSlots = 3))
                assertEquals(
                    StoredPendingRewardedCreation(42L, 4, true, "study_plan_v1"),
                    bandalartDataStore.getPendingRewardedCreation(),
                )

                bandalartDataStore.clearPendingRewardedCreation(42L)
                assertEquals(null, bandalartDataStore.getPendingRewardedCreation())
            }

        @Test
        @DisplayName("템플릿 키가 없는 구버전 보상 요청은 빈 생성으로 복구해야 한다")
        fun legacyRewardedCreationWithoutTemplateRemainsCompatible() =
            runTest {
                bandalartDataStore.prepareRewardedCreation(
                    requestId = 43L,
                    minimumSlots = 3,
                )

                assertEquals(
                    StoredPendingRewardedCreation(
                        requestId = 43L,
                        targetSlots = 4,
                        isGranted = true,
                        templateId = null,
                    ),
                    bandalartDataStore.grantRewardedCreation(43L),
                )
            }

        @Test
        @DisplayName("중복 grant는 슬롯을 다시 늘리지 않아야 한다")
        fun duplicateRewardedGrantIsIdempotent() =
            runTest {
                bandalartDataStore.resolveMaxBandalartSlots(minimumSlots = 3)
                bandalartDataStore.prepareRewardedCreation(requestId = 7L, minimumSlots = 3)

                bandalartDataStore.grantRewardedCreation(7L)
                bandalartDataStore.grantRewardedCreation(7L)

                assertEquals(4, bandalartDataStore.resolveMaxBandalartSlots(minimumSlots = 3))
            }
    }

    @Nested
    @DisplayName("최근 사용 이모지 관련 테스트")
    inner class RecentEmojisTest {
        @Test
        @DisplayName("초기값은 빈 목록이어야 한다")
        fun recentEmojisAreEmptyByDefault() =
            runTest {
                assertEquals(emptyList<String>(), bandalartDataStore.recentEmojis.first())
            }

        @Test
        @DisplayName("중복을 제거하고 최신 사용 순으로 저장해야 한다")
        fun recentEmojisAreDeduplicatedAndMovedToFront() =
            runTest {
                bandalartDataStore.addRecentEmoji("🎯")
                bandalartDataStore.addRecentEmoji("🚀")
                bandalartDataStore.addRecentEmoji("🎯")

                assertEquals(listOf("🎯", "🚀"), bandalartDataStore.recentEmojis.first())
            }

        @Test
        @DisplayName("최근 12개까지만 저장해야 한다")
        fun recentEmojisAreLimitedToTwelveItems() =
            runTest {
                for (index in 1..13) {
                    bandalartDataStore.addRecentEmoji("emoji-$index")
                }

                assertEquals(
                    (13 downTo 2).map { index -> "emoji-$index" },
                    bandalartDataStore.recentEmojis.first(),
                )
            }
    }

    @Nested
    @DisplayName("완료된 반다라트 목록 관련 테스트")
    inner class CompletedBandalartListTest {
        @Test
        @DisplayName("반다라트 ID를 추가하고 목록을 조회할 수 있어야 한다")
        fun testUpsertAndGetBandalartList() =
            runTest {
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
        fun testUpdateExistingBandalartId() =
            runTest {
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
        fun testCheckCompletedBandalartId() =
            runTest {
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
        fun testDeleteBandalartId() =
            runTest {
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
        fun testOnboardingCompletedStatus() =
            runTest {
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

    @Nested
    @DisplayName("테마 설정 관련 테스트")
    inner class ThemeModeTest {
        @Test
        @DisplayName("설정값이 없으면 null을 방출하고 저장 후 최신 값을 방출해야 한다")
        fun themeModeIsPersisted() =
            runTest {
                assertEquals(null, bandalartDataStore.themeMode.first())

                bandalartDataStore.setThemeMode("dark")

                assertEquals("dark", bandalartDataStore.themeMode.first())
            }
    }
}
