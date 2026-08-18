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

package com.nexters.bandalart.core.domain.backup

interface CloudBackupRepository {
    val isSupported: Boolean

    suspend fun hasLocalData(): Boolean

    suspend fun findBackup(): BackupMetadata?

    suspend fun createBackup(): BackupMetadata

    suspend fun restoreBackup(): BackupMetadata?
}

fun interface DeviceBackupKeyProvider {
    fun getDeviceKey(): String?
}

fun interface StartupBackupPolicy {
    suspend fun evaluate(): StartupBackupDecision
}

sealed interface StartupBackupDecision {
    data object Continue : StartupBackupDecision

    data class OfferRestore(
        val metadata: BackupMetadata,
    ) : StartupBackupDecision
}
