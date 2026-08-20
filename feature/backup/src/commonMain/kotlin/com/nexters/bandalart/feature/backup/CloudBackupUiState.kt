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

package com.nexters.bandalart.feature.backup

import com.nexters.bandalart.core.domain.backup.BackupMetadata
import com.nexters.bandalart.core.common.RewardedAdResult
import com.nexters.bandalart.core.navigation.CloudBackupScreen
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class CloudBackupUiState(
    val entryPoint: CloudBackupScreen.EntryPoint,
    val isSupported: Boolean,
    val isLoading: Boolean,
    val metadata: BackupMetadata?,
    val showCreateBackupConfirmation: Boolean,
    val showRestoreConfirmation: Boolean,
    val rewardedAdRequestId: Long?,
    val effect: Effect?,
    val result: Result?,
    val eventSink: (Event) -> Unit,
) : CircuitUiState {
    enum class Result {
        BACKUP_CREATED,
        RESTORED,
        BACKUP_NOT_FOUND,
        ERROR,
    }

    sealed interface Effect {
        data object ShowAdUnavailableSnackbar : Effect
    }

    sealed interface Event : CircuitUiEvent {
        data object Back : Event

        data object CreateBackup : Event

        data object ConfirmCreateBackup : Event

        data object DismissCreateBackupConfirmation : Event

        data class RewardedAdFinished(
            val requestId: Long,
            val result: RewardedAdResult,
        ) : Event

        data object RestoreBackup : Event

        data object ConfirmRestore : Event

        data object DismissRestoreConfirmation : Event

        data object StartFresh : Event

        data object ConsumeResult : Event

        data object ConsumeEffect : Event
    }
}
