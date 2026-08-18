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
import com.nexters.bandalart.core.domain.backup.BackupPreferences
import com.nexters.bandalart.core.domain.backup.BackupSnapshot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SupabaseBackupRemoteDataSourceTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun getBackupDecodesTheJsonPayloadAndMetadata() =
        runTest {
            val snapshot = snapshot()
            val rpcClient =
                FakeBackupRpcClient(
                    getResult =
                        BackupRpcRow(
                            schemaVersion = 1,
                            payload = json.encodeToJsonElement(snapshot).jsonObject,
                            bandalartCount = 1,
                            updatedAt = "2026-08-18T01:02:03Z",
                        ),
                )

            val backup = SupabaseBackupRemoteDataSource(rpcClient, json).getBackup(DEVICE_KEY)

            assertEquals(snapshot, backup?.snapshot)
            assertEquals(1, backup?.metadata?.bandalartCount)
            assertEquals("2026-08-18T01:02:03Z", backup?.metadata?.updatedAt)
        }

    @Test
    fun getBackupReturnsNullWhenTheRpcHasNoRow() =
        runTest {
            assertNull(SupabaseBackupRemoteDataSource(FakeBackupRpcClient(), json).getBackup(DEVICE_KEY))
        }

    @Test
    fun putBackupSendsVersionedJsonAndReturnsServerMetadata() =
        runTest {
            val rpcClient =
                FakeBackupRpcClient(
                    putResult = BackupRpcMetadataRow(1, 1, "2026-08-18T01:02:03Z"),
                )
            val snapshot = snapshot()

            val metadata = SupabaseBackupRemoteDataSource(rpcClient, json).putBackup(DEVICE_KEY, snapshot)

            assertEquals(DEVICE_KEY, rpcClient.putDeviceKey)
            assertEquals(1, rpcClient.putSchemaVersion)
            assertEquals(json.encodeToJsonElement(snapshot).jsonObject, rpcClient.putPayload)
            assertEquals(1, rpcClient.putBandalartCount)
            assertEquals("2026-08-18T01:02:03Z", metadata.updatedAt)
        }

    private fun snapshot() =
        BackupSnapshot(
            bandalarts = listOf(BackupBandalart(id = 1L, mainColor = "#FF3FFFBA", subColor = "#FF111827")),
            cells = emptyList(),
            preferences =
                BackupPreferences(
                    recentBandalartId = 1L,
                    recentSubGoalIds = emptyMap(),
                    completedBandalarts = emptyList(),
                    onboardingCompleted = true,
                    themeMode = null,
                    recentEmojis = emptyList(),
                    deadlineReminderEnabled = false,
                    maxBandalartSlots = 1,
                ),
        )

    private class FakeBackupRpcClient(
        private val getResult: BackupRpcRow? = null,
        private val putResult: BackupRpcMetadataRow = BackupRpcMetadataRow(1, 0, "2026-08-18T00:00:00Z"),
    ) : BackupRpcClient {
        var putDeviceKey: String? = null
        var putSchemaVersion: Int? = null
        var putPayload: kotlinx.serialization.json.JsonObject? = null
        var putBandalartCount: Int? = null

        override suspend fun getBackup(deviceKey: String): BackupRpcRow? = getResult

        override suspend fun putBackup(
            deviceKey: String,
            schemaVersion: Int,
            payload: kotlinx.serialization.json.JsonObject,
            bandalartCount: Int,
        ): BackupRpcMetadataRow {
            putDeviceKey = deviceKey
            putSchemaVersion = schemaVersion
            putPayload = payload
            putBandalartCount = bandalartCount
            return putResult
        }
    }

    private companion object {
        private const val DEVICE_KEY = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    }
}
