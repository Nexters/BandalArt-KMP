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

package com.nexters.bandalart.core.data.backup

import com.nexters.bandalart.core.domain.backup.BackupBandalart
import com.nexters.bandalart.core.domain.backup.BackupCell
import com.nexters.bandalart.core.domain.backup.BackupMetadata
import com.nexters.bandalart.core.domain.backup.BackupPreferences
import com.nexters.bandalart.core.domain.backup.BackupSnapshot
import com.nexters.bandalart.core.domain.backup.DeviceBackupKeyProvider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DefaultCloudBackupRepositoryTest {
    @Test
    fun createBackupUploadsTheCurrentLocalSnapshotForThisDevice() =
        runTest {
            val snapshot = validSnapshot()
            val local = FakeBackupLocalDataSource(snapshot = snapshot)
            val remote = FakeBackupRemoteDataSource()
            val repository =
                DefaultCloudBackupRepository(
                    deviceKeyProvider = FixedDeviceBackupKeyProvider(DEVICE_KEY),
                    localDataSource = local,
                    remoteDataSource = remote,
                )

            val metadata = repository.createBackup()

            assertEquals(DEVICE_KEY, remote.lastPutDeviceKey)
            assertEquals(snapshot, remote.lastPutSnapshot)
            assertEquals(1, metadata.bandalartCount)
        }

    @Test
    fun restoreBackupReplacesLocalDataOnlyAfterTheRemoteSnapshotIsValid() =
        runTest {
            val invalidSnapshot =
                validSnapshot().copy(
                    cells = validSnapshot().cells.map { it.copy(bandalartId = 999L) },
                )
            val local = FakeBackupLocalDataSource(snapshot = validSnapshot())
            val remote =
                FakeBackupRemoteDataSource(
                    storedBackup = RemoteBackup(invalidSnapshot, METADATA),
                )
            val repository =
                DefaultCloudBackupRepository(
                    deviceKeyProvider = FixedDeviceBackupKeyProvider(DEVICE_KEY),
                    localDataSource = local,
                    remoteDataSource = remote,
                )

            val failure = runCatching { repository.restoreBackup() }.exceptionOrNull()

            assertTrue(failure is InvalidBackupSnapshotException)
            assertNull(local.restoredSnapshot)
        }

    @Test
    fun missingRemoteBackupDoesNotMutateLocalData() =
        runTest {
            val local = FakeBackupLocalDataSource(snapshot = validSnapshot())
            val repository =
                DefaultCloudBackupRepository(
                    deviceKeyProvider = FixedDeviceBackupKeyProvider(DEVICE_KEY),
                    localDataSource = local,
                    remoteDataSource = FakeBackupRemoteDataSource(),
                )

            assertNull(repository.restoreBackup())
            assertNull(local.restoredSnapshot)
        }

    @Test
    fun unavailableDeviceIdentityDisablesBackupWithoutCallingRemote() =
        runTest {
            val remote = FakeBackupRemoteDataSource()
            val repository =
                DefaultCloudBackupRepository(
                    deviceKeyProvider = FixedDeviceBackupKeyProvider(null),
                    localDataSource = FakeBackupLocalDataSource(validSnapshot()),
                    remoteDataSource = remote,
                )

            assertFalse(repository.isSupported)
            assertNull(repository.findBackup())
            assertNull(remote.lastGetDeviceKey)
        }

    private fun validSnapshot() =
        BackupSnapshot(
            bandalarts =
                listOf(
                    BackupBandalart(
                        id = 1L,
                        title = "건강한 생활",
                        mainColor = "#FF3FFFBA",
                        subColor = "#FF111827",
                    ),
                ),
            cells =
                listOf(
                    BackupCell(
                        id = 10L,
                        bandalartId = 1L,
                        title = "건강한 생활",
                    ),
                ),
            preferences =
                BackupPreferences(
                    recentBandalartId = 1L,
                    recentSubGoalIds = emptyMap(),
                    completedBandalarts = emptyList(),
                    onboardingCompleted = true,
                    themeMode = "system",
                    recentEmojis = listOf("🎯"),
                    deadlineReminderEnabled = false,
                    maxBandalartSlots = 1,
                ),
        )

    private class FixedDeviceBackupKeyProvider(
        private val key: String?,
    ) : DeviceBackupKeyProvider {
        override fun getDeviceKey(): String? = key
    }

    private class FakeBackupLocalDataSource(
        private val snapshot: BackupSnapshot,
    ) : BackupLocalDataSource {
        var restoredSnapshot: BackupSnapshot? = null

        override suspend fun hasData(): Boolean = snapshot.bandalarts.isNotEmpty()

        override suspend fun createSnapshot(): BackupSnapshot = snapshot

        override suspend fun restoreSnapshot(snapshot: BackupSnapshot) {
            restoredSnapshot = snapshot
        }
    }

    private class FakeBackupRemoteDataSource(
        private val storedBackup: RemoteBackup? = null,
    ) : BackupRemoteDataSource {
        var lastGetDeviceKey: String? = null
        var lastPutDeviceKey: String? = null
        var lastPutSnapshot: BackupSnapshot? = null

        override suspend fun getBackup(deviceKey: String): RemoteBackup? {
            lastGetDeviceKey = deviceKey
            return storedBackup
        }

        override suspend fun putBackup(
            deviceKey: String,
            snapshot: BackupSnapshot,
        ): BackupMetadata {
            lastPutDeviceKey = deviceKey
            lastPutSnapshot = snapshot
            return METADATA
        }
    }

    private companion object {
        private const val DEVICE_KEY = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        private val METADATA = BackupMetadata(bandalartCount = 1, updatedAt = "2026-08-18T00:00:00Z")
    }
}
