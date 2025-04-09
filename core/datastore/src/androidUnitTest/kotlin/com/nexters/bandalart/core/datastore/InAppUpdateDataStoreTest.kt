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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

object VersionTestData {
    @JvmStatic
    fun versionProvider() =
        listOf(
            Arguments.of(1, 0, 0, 10000), // 1.0.0
            Arguments.of(2, 2, 0, 20200), // 2.2.0
            Arguments.of(2, 2, 5, 20205), // 2.2.5
            Arguments.of(3, 1, 2, 30102), // 3.1.2
            Arguments.of(10, 5, 3, 100503) // 10.5.3
        )
}

@DisplayName("InAppUpdateDataStore 테스트")
class InAppUpdateDataStoreTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var inAppUpdateDataStore: InAppUpdateDataStore

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        // 테스트용 임시 DataStore 생성
        dataStore =
            PreferenceDataStoreFactory.create(
                produceFile = { tempDir.resolve("test_update_datastore.preferences_pb") }
            )
        inAppUpdateDataStore = InAppUpdateDataStore(dataStore)
    }

    @Nested
    @DisplayName("거부된 업데이트 버전 코드 테스트")
    inner class RejectedVersionTest {
        @Test
        @DisplayName("거부된 업데이트 버전 코드를 설정하고 가져올 수 있어야 한다")
        fun testSetAndGetLastRejectedUpdateVersion() =
            runTest {
                // given - 버전 2.2.0에 해당하는 버전 코드
                val versionCode = calculateVersionCode(2, 2, 0)

                // when
                inAppUpdateDataStore.setLastRejectedUpdateVersion(versionCode)
                val retrievedVersionCode = inAppUpdateDataStore.getLastRejectedUpdateVersion()

                // then
                assertEquals(versionCode, retrievedVersionCode)
            }

        @Test
        @DisplayName("거부된 업데이트 버전이 설정되지 않았을 때 기본값 0을 반환해야 한다")
        fun testDefaultLastRejectedUpdateVersion() =
            runTest {
                // when
                val retrievedVersionCode = inAppUpdateDataStore.getLastRejectedUpdateVersion()

                // then
                assertEquals(0, retrievedVersionCode)
            }

        @ParameterizedTest(name = "버전 {0}.{1}.{2}의 거부 코드 {3}이 저장되어야 한다")
        @MethodSource("com.nexters.bandalart.core.datastore.VersionTestData#versionProvider")
        @DisplayName("다양한 버전 코드를 정확히 저장하고 불러올 수 있어야 한다")
        fun testDifferentVersionCodes(
            major: Int,
            minor: Int,
            patch: Int,
            expectedVersionCode: Int
        ) = runTest {
            // given
            val versionCode = calculateVersionCode(major, minor, patch)
            assertEquals(expectedVersionCode, versionCode) // 버전 코드 계산 정확성 확인

            // when
            inAppUpdateDataStore.setLastRejectedUpdateVersion(versionCode)
            val retrievedVersionCode = inAppUpdateDataStore.getLastRejectedUpdateVersion()

            // then
            assertEquals(versionCode, retrievedVersionCode)
        }

        @Test
        @DisplayName("새 버전이 출시되면 이전에 거부한 버전을 무시할 수 있어야 한다")
        fun testIgnorePreviousRejectedVersion() =
            runTest {
                // given
                val oldVersionCode = calculateVersionCode(2, 2, 0)
                val newVersionCode = calculateVersionCode(2, 3, 0)

                // when: 이전 버전을 거부했다고 가정
                inAppUpdateDataStore.setLastRejectedUpdateVersion(oldVersionCode)
                val retrievedOldVersion = inAppUpdateDataStore.getLastRejectedUpdateVersion()

                // then: 이전 버전이 제대로 저장됨
                assertEquals(oldVersionCode, retrievedOldVersion)

                // 새 버전 출시 시나리오: 새 버전 코드 > 거부된 버전 코드
                val shouldShowUpdate = newVersionCode > retrievedOldVersion
                assertTrue(shouldShowUpdate)
            }

        @Test
        @DisplayName("마이너 버전 업데이트가 메이저 버전보다 작은 버전 코드를 가져야 한다")
        fun testVersionCodeHierarchy() =
            runTest {
                // given
                val majorUpdateVersionCode = calculateVersionCode(3, 0, 0)
                val minorUpdateVersionCode = calculateVersionCode(2, 3, 0)
                val patchUpdateVersionCode = calculateVersionCode(2, 2, 1)

                // then
                assertTrue(majorUpdateVersionCode > minorUpdateVersionCode)
                assertTrue(minorUpdateVersionCode > patchUpdateVersionCode)
            }
    }

    private fun calculateVersionCode(
        major: Int,
        minor: Int,
        patch: Int
    ): Int = (major * 10000) + (minor * 100) + patch
}
