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

import com.nexters.bandalart.core.domain.backup.CloudBackupRepository
import com.nexters.bandalart.core.domain.backup.StartupBackupDecision
import com.nexters.bandalart.core.domain.backup.StartupBackupPolicy
import kotlinx.coroutines.CancellationException

class DefaultStartupBackupPolicy(
    private val repository: CloudBackupRepository,
) : StartupBackupPolicy {
    override suspend fun evaluate(): StartupBackupDecision =
        try {
            if (!repository.isSupported) {
                StartupBackupDecision.Continue
            } else {
                val hasLocalData = repository.hasLocalData()
                if (hasLocalData) {
                    StartupBackupDecision.Continue
                } else {
                    val backup = repository.findBackup()
                    if (backup == null) {
                        StartupBackupDecision.Continue
                    } else {
                        StartupBackupDecision.OfferRestore(backup)
                    }
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            StartupBackupDecision.Continue
        }
}
