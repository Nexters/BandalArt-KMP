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

import com.nexters.bandalart.core.domain.backup.BackupMetadata
import com.nexters.bandalart.core.domain.backup.CloudBackupRepository
import com.nexters.bandalart.core.domain.backup.StartupBackupDecision
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DefaultStartupBackupPolicyTest {
    @Test
    fun localDataSkipsTheRemoteBackupCheck() =
        runTest {
            val repository = FakeCloudBackupRepository(hasLocalData = true, backup = METADATA)

            assertEquals(StartupBackupDecision.Continue, DefaultStartupBackupPolicy(repository).evaluate())
            assertFalse(repository.findBackupCalled)
        }

    @Test
    fun emptyLocalDataOffersTheRemoteBackup() =
        runTest {
            val repository = FakeCloudBackupRepository(hasLocalData = false, backup = METADATA)

            assertEquals(
                StartupBackupDecision.OfferRestore(METADATA),
                DefaultStartupBackupPolicy(repository).evaluate(),
            )
            assertTrue(repository.findBackupCalled)
        }

    @Test
    fun remoteFailureDoesNotBlockStartup() =
        runTest {
            val repository = FakeCloudBackupRepository(hasLocalData = false, failure = IllegalStateException("offline"))

            assertEquals(StartupBackupDecision.Continue, DefaultStartupBackupPolicy(repository).evaluate())
        }

    private class FakeCloudBackupRepository(
        private val hasLocalData: Boolean,
        private val backup: BackupMetadata? = null,
        private val failure: Throwable? = null,
    ) : CloudBackupRepository {
        var findBackupCalled = false
        override val isSupported = true

        override suspend fun hasLocalData(): Boolean = hasLocalData

        override suspend fun findBackup(): BackupMetadata? {
            findBackupCalled = true
            failure?.let { throw it }
            return backup
        }

        override suspend fun createBackup(): BackupMetadata = error("Not used")

        override suspend fun restoreBackup(): BackupMetadata? = error("Not used")
    }

    private companion object {
        private val METADATA = BackupMetadata(2, "2026-08-18T01:02:03Z")
    }
}
