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

package com.nexters.bandalart.feature.backup.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.nexters.bandalart.core.domain.backup.BackupMetadata
import com.nexters.bandalart.core.domain.backup.CloudBackupRepository
import com.nexters.bandalart.core.domain.notification.DeadlineReminderReconciler
import com.nexters.bandalart.core.domain.notification.NoOpDeadlineReminderReconciler
import com.nexters.bandalart.core.navigation.CloudBackupScreen
import com.nexters.bandalart.feature.backup.CloudBackupUiState
import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.onboarding.OnboardingScreen
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@AssistedInject
class CloudBackupPresenter(
    @Assisted private val screen: CloudBackupScreen,
    @Assisted private val navigator: Navigator,
    private val repository: CloudBackupRepository,
    private val deadlineReminderReconciler: DeadlineReminderReconciler = NoOpDeadlineReminderReconciler,
) : Presenter<CloudBackupUiState> {
    @Composable
    override fun present(): CloudBackupUiState {
        val scope = rememberCoroutineScope()
        var metadata by remember {
            mutableStateOf(
                screen.backupCount?.let { count ->
                    BackupMetadata(count, screen.backupUpdatedAt.orEmpty())
                },
            )
        }
        var isLoading by remember { mutableStateOf(screen.entryPoint == CloudBackupScreen.EntryPoint.SETTINGS) }
        var showRestoreConfirmation by remember { mutableStateOf(false) }
        var result by remember { mutableStateOf<CloudBackupUiState.Result?>(null) }

        LaunchedEffect(screen.entryPoint, repository.isSupported) {
            if (screen.entryPoint == CloudBackupScreen.EntryPoint.SETTINGS && repository.isSupported) {
                try {
                    metadata = repository.findBackup()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    result = CloudBackupUiState.Result.ERROR
                } finally {
                    isLoading = false
                }
            } else {
                isLoading = false
            }
        }

        fun navigateToFallback() {
            navigator.resetRoot(
                when (screen.fallback) {
                    CloudBackupScreen.Fallback.HOME -> HomeScreen
                    CloudBackupScreen.Fallback.ONBOARDING -> OnboardingScreen
                },
            )
        }

        fun restore() {
            isLoading = true
            showRestoreConfirmation = false
            result = null
            scope.launch {
                try {
                    val restored = repository.restoreBackup()
                    if (restored == null) {
                        result = CloudBackupUiState.Result.BACKUP_NOT_FOUND
                    } else {
                        deadlineReminderReconciler.reconcileAll()
                        if (screen.entryPoint == CloudBackupScreen.EntryPoint.STARTUP) {
                            navigator.resetRoot(HomeScreen)
                        } else {
                            metadata = restored
                            result = CloudBackupUiState.Result.RESTORED
                        }
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    result = CloudBackupUiState.Result.ERROR
                } finally {
                    isLoading = false
                }
            }
        }

        return CloudBackupUiState(
            entryPoint = screen.entryPoint,
            isSupported = repository.isSupported,
            isLoading = isLoading,
            metadata = metadata,
            showRestoreConfirmation = showRestoreConfirmation,
            result = result,
        ) { event ->
            when (event) {
                CloudBackupUiState.Event.Back -> {
                    if (screen.entryPoint == CloudBackupScreen.EntryPoint.SETTINGS) navigator.pop() else navigateToFallback()
                }
                CloudBackupUiState.Event.CreateBackup -> {
                    isLoading = true
                    result = null
                    scope.launch {
                        try {
                            metadata = repository.createBackup()
                            result = CloudBackupUiState.Result.BACKUP_CREATED
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (_: Exception) {
                            result = CloudBackupUiState.Result.ERROR
                        } finally {
                            isLoading = false
                        }
                    }
                }
                CloudBackupUiState.Event.RestoreBackup -> {
                    if (screen.entryPoint == CloudBackupScreen.EntryPoint.SETTINGS) {
                        showRestoreConfirmation = true
                    } else {
                        restore()
                    }
                }
                CloudBackupUiState.Event.ConfirmRestore -> restore()
                CloudBackupUiState.Event.DismissRestoreConfirmation -> showRestoreConfirmation = false
                CloudBackupUiState.Event.StartFresh -> navigateToFallback()
                CloudBackupUiState.Event.ConsumeResult -> result = null
            }
        }
    }

    @CircuitInject(CloudBackupScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(
            @Assisted screen: CloudBackupScreen,
            @Assisted navigator: Navigator,
        ): CloudBackupPresenter
    }
}
