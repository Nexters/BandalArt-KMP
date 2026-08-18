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
import com.nexters.bandalart.core.domain.backup.CloudBackupRepository
import com.nexters.bandalart.core.domain.backup.DeviceBackupKeyProvider

class DefaultCloudBackupRepository(
    private val deviceKeyProvider: DeviceBackupKeyProvider,
    private val localDataSource: BackupLocalDataSource,
    private val remoteDataSource: BackupRemoteDataSource,
) : CloudBackupRepository {
    override val isSupported: Boolean
        get() = deviceKeyProvider.getDeviceKey() != null

    override suspend fun hasLocalData(): Boolean = localDataSource.hasData()

    override suspend fun findBackup(): BackupMetadata? {
        val deviceKey = deviceKeyProvider.getDeviceKey() ?: return null
        return remoteDataSource.getBackup(deviceKey)?.metadata
    }

    override suspend fun createBackup(): BackupMetadata {
        val deviceKey = requireDeviceKey()
        val snapshot = localDataSource.createSnapshot()
        snapshot.validate()
        return remoteDataSource.putBackup(deviceKey, snapshot)
    }

    override suspend fun restoreBackup(): BackupMetadata? {
        val deviceKey = requireDeviceKey()
        val remoteBackup = remoteDataSource.getBackup(deviceKey) ?: return null
        remoteBackup.snapshot.validate()
        localDataSource.restoreSnapshot(remoteBackup.snapshot)
        return remoteBackup.metadata
    }

    private fun requireDeviceKey(): String =
        checkNotNull(deviceKeyProvider.getDeviceKey()) {
            "Cloud backup is not supported on this device"
        }
}

interface BackupLocalDataSource {
    suspend fun hasData(): Boolean

    suspend fun createSnapshot(): BackupSnapshot

    suspend fun restoreSnapshot(snapshot: BackupSnapshot)
}

interface BackupRemoteDataSource {
    suspend fun getBackup(deviceKey: String): RemoteBackup?

    suspend fun putBackup(
        deviceKey: String,
        snapshot: BackupSnapshot,
    ): BackupMetadata
}

data class RemoteBackup(
    val snapshot: BackupSnapshot,
    val metadata: BackupMetadata,
)

class InvalidBackupSnapshotException(
    message: String,
) : IllegalArgumentException(message)

private fun BackupSnapshot.validate() {
    if (schemaVersion != BackupSnapshot.CURRENT_SCHEMA_VERSION) {
        throw InvalidBackupSnapshotException("Unsupported backup schema version: $schemaVersion")
    }
    val bandalartIds = bandalarts.map { it.id }
    if (bandalartIds.any { it <= 0L } || bandalartIds.distinct().size != bandalartIds.size) {
        throw InvalidBackupSnapshotException("Bandalart IDs must be unique positive values")
    }
    val cellsById = cells.associateBy { it.id }
    if (cells.any { it.id <= 0L } || cellsById.size != cells.size) {
        throw InvalidBackupSnapshotException("Cell IDs must be unique positive values")
    }
    val bandalartIdSet = bandalartIds.toSet()
    cells.forEach { cell ->
        if (cell.bandalartId !in bandalartIdSet) {
            throw InvalidBackupSnapshotException("Cell references a missing bandalart")
        }
        cell.parentId?.let { parentId ->
            val parent = cellsById[parentId]
            if (parent == null || parent.bandalartId != cell.bandalartId || parentId == cell.id) {
                throw InvalidBackupSnapshotException("Cell references an invalid parent")
            }
        }
    }
    if (preferences.recentBandalartId != 0L && preferences.recentBandalartId !in bandalartIdSet) {
        throw InvalidBackupSnapshotException("Recent bandalart is missing from the snapshot")
    }
    if (preferences.maxBandalartSlots < bandalarts.size) {
        throw InvalidBackupSnapshotException("Backup slot count is smaller than its bandalart count")
    }
}
