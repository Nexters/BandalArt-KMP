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
import com.nexters.bandalart.core.domain.backup.BackupSnapshot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

class SupabaseBackupRemoteDataSource(
    private val rpcClient: BackupRpcClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : BackupRemoteDataSource {
    override suspend fun getBackup(deviceKey: String): RemoteBackup? {
        val row = rpcClient.getBackup(deviceKey) ?: return null
        val snapshot = json.decodeFromJsonElement<BackupSnapshot>(row.payload)
        if (snapshot.schemaVersion != row.schemaVersion) {
            throw InvalidBackupSnapshotException("Backup schema version does not match its payload")
        }
        return RemoteBackup(
            snapshot = snapshot,
            metadata = BackupMetadata(row.bandalartCount, row.updatedAt),
        )
    }

    override suspend fun putBackup(
        deviceKey: String,
        snapshot: BackupSnapshot,
    ): BackupMetadata {
        val row =
            rpcClient.putBackup(
                deviceKey = deviceKey,
                schemaVersion = snapshot.schemaVersion,
                payload = json.encodeToJsonElement(snapshot).jsonObject,
                bandalartCount = snapshot.bandalarts.size,
            )
        return BackupMetadata(row.bandalartCount, row.updatedAt)
    }
}

interface BackupRpcClient {
    suspend fun getBackup(deviceKey: String): BackupRpcRow?

    suspend fun putBackup(
        deviceKey: String,
        schemaVersion: Int,
        payload: JsonObject,
        bandalartCount: Int,
    ): BackupRpcMetadataRow
}

@Serializable
data class BackupRpcRow(
    @SerialName("schema_version")
    val schemaVersion: Int,
    val payload: JsonObject,
    @SerialName("bandalart_count")
    val bandalartCount: Int,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class BackupRpcMetadataRow(
    @SerialName("schema_version")
    val schemaVersion: Int,
    @SerialName("bandalart_count")
    val bandalartCount: Int,
    @SerialName("updated_at")
    val updatedAt: String,
)
