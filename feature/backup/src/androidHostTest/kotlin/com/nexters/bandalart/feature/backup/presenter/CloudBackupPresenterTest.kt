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

import com.nexters.bandalart.core.common.RewardedAdResult
import com.nexters.bandalart.core.domain.backup.BackupMetadata
import com.nexters.bandalart.core.domain.backup.CloudBackupRepository
import com.nexters.bandalart.core.domain.notification.DeadlineReminderReconciler
import com.nexters.bandalart.core.domain.notification.DeadlineReminderSchedulingHealth
import com.nexters.bandalart.core.navigation.CloudBackupScreen
import com.nexters.bandalart.feature.backup.CloudBackupUiState
import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.onboarding.OnboardingScreen
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CloudBackupPresenterTest {
    @Test
    fun settingsLoadsExistingBackup() =
        runTest {
            val metadata = BackupMetadata(2, "2026-08-18T01:00:00Z")
            val repository = FakeCloudBackupRepository(existing = metadata)
            val presenter = presenter(CloudBackupScreen.EntryPoint.SETTINGS, repository)

            presenter.test {
                awaitItem()
                assertEquals(metadata, awaitItem().metadata)
            }
        }

    @Test
    fun settingsRestoreRequiresConfirmation() =
        runTest {
            val repository = FakeCloudBackupRepository(existing = BackupMetadata(1, "now"))
            val presenter = presenter(CloudBackupScreen.EntryPoint.SETTINGS, repository)

            presenter.test {
                awaitItem()
                val state = awaitItem()
                state.eventSink(CloudBackupUiState.Event.RestoreBackup)
                val confirmation = awaitItem()

                assertTrue(confirmation.showRestoreConfirmation)
                assertEquals(0, repository.restoreCalls)

                confirmation.eventSink(CloudBackupUiState.Event.ConfirmRestore)
                var restored = awaitItem()
                while (restored.result != CloudBackupUiState.Result.RESTORED) restored = awaitItem()
                assertFalse(restored.showRestoreConfirmation)
                assertEquals(CloudBackupUiState.Result.RESTORED, restored.result)
                assertEquals(1, repository.restoreCalls)
            }
        }

    @Test
    fun settingsCreateBackupRequiresRewardedAdConfirmation() =
        runTest {
            val repository = FakeCloudBackupRepository()
            val presenter = presenter(CloudBackupScreen.EntryPoint.SETTINGS, repository)

            presenter.test {
                awaitItem()
                val state = awaitItem()
                state.eventSink(CloudBackupUiState.Event.CreateBackup)
                val confirmation = awaitItem()

                assertTrue(confirmation.showCreateBackupConfirmation)
                assertEquals(0, repository.createCalls)
            }
        }

    @Test
    fun rewardedAdCreatesBackupExactlyOnce() =
        runTest {
            val repository = FakeCloudBackupRepository()
            val presenter = presenter(CloudBackupScreen.EntryPoint.SETTINGS, repository)

            presenter.test {
                awaitItem()
                awaitItem().eventSink(CloudBackupUiState.Event.CreateBackup)
                val confirmation = awaitItem()
                confirmation.eventSink(CloudBackupUiState.Event.ConfirmCreateBackup)
                var awaitingAd = awaitItem()
                while (awaitingAd.rewardedAdRequestId == null) awaitingAd = awaitItem()
                val requestId = requireNotNull(awaitingAd.rewardedAdRequestId)

                awaitingAd.eventSink(
                    CloudBackupUiState.Event.RewardedAdFinished(requestId, RewardedAdResult.REWARDED),
                )
                var completed = awaitItem()
                while (completed.result != CloudBackupUiState.Result.BACKUP_CREATED) completed = awaitItem()

                completed.eventSink(
                    CloudBackupUiState.Event.RewardedAdFinished(requestId, RewardedAdResult.REWARDED),
                )
                yield()
                assertEquals(1, repository.createCalls)
            }
        }

    @Test
    fun dismissedRewardedAdDoesNotCreateBackup() =
        runTest {
            val repository = FakeCloudBackupRepository()
            val presenter = presenter(CloudBackupScreen.EntryPoint.SETTINGS, repository)

            presenter.test {
                awaitItem()
                awaitItem().eventSink(CloudBackupUiState.Event.CreateBackup)
                val confirmation = awaitItem()
                confirmation.eventSink(CloudBackupUiState.Event.ConfirmCreateBackup)
                var awaitingAd = awaitItem()
                while (awaitingAd.rewardedAdRequestId == null) awaitingAd = awaitItem()
                val requestId = requireNotNull(awaitingAd.rewardedAdRequestId)

                awaitingAd.eventSink(
                    CloudBackupUiState.Event.RewardedAdFinished(requestId, RewardedAdResult.DISMISSED),
                )
                val dismissed = awaitItem()

                assertEquals(null, dismissed.rewardedAdRequestId)
                assertEquals(0, repository.createCalls)
            }
        }

    @Test
    fun restoreIsIgnoredWhileRewardedAdIsActive() =
        runTest {
            val repository = FakeCloudBackupRepository(existing = BackupMetadata(1, "now"))
            val presenter = presenter(CloudBackupScreen.EntryPoint.SETTINGS, repository)

            presenter.test {
                awaitItem()
                awaitItem().eventSink(CloudBackupUiState.Event.CreateBackup)
                val confirmation = awaitItem()
                confirmation.eventSink(CloudBackupUiState.Event.ConfirmCreateBackup)
                var awaitingAd = awaitItem()
                while (awaitingAd.rewardedAdRequestId == null) awaitingAd = awaitItem()

                awaitingAd.eventSink(CloudBackupUiState.Event.RestoreBackup)
                yield()

                expectNoEvents()
                assertFalse(awaitingAd.showRestoreConfirmation)
                assertEquals(0, repository.restoreCalls)
            }
        }

    @Test
    fun failedRewardedAdShowsUnavailableMessageAndCreatesBackup() =
        runTest {
            val repository = FakeCloudBackupRepository()
            val presenter = presenter(CloudBackupScreen.EntryPoint.SETTINGS, repository)

            presenter.test {
                awaitItem()
                awaitItem().eventSink(CloudBackupUiState.Event.CreateBackup)
                val confirmation = awaitItem()
                confirmation.eventSink(CloudBackupUiState.Event.ConfirmCreateBackup)
                var awaitingAd = awaitItem()
                while (awaitingAd.rewardedAdRequestId == null) awaitingAd = awaitItem()
                val requestId = requireNotNull(awaitingAd.rewardedAdRequestId)

                awaitingAd.eventSink(
                    CloudBackupUiState.Event.RewardedAdFinished(requestId, RewardedAdResult.FAILED),
                )
                var completed = awaitItem()
                while (completed.result != CloudBackupUiState.Result.BACKUP_CREATED) completed = awaitItem()

                assertEquals(CloudBackupUiState.Effect.ShowAdUnavailableSnackbar, completed.effect)
                assertEquals(1, repository.createCalls)
            }
        }

    @Test
    fun startupRestoreRestoresImmediatelyAndOpensHome() =
        runTest {
            val repository = FakeCloudBackupRepository(existing = BackupMetadata(1, "now"))
            val screen =
                CloudBackupScreen(
                    entryPoint = CloudBackupScreen.EntryPoint.STARTUP,
                    backupCount = 1,
                    backupUpdatedAt = "now",
                )
            val navigator = FakeNavigator(screen)
            val reconciler = RecordingReconciler()
            val presenter = CloudBackupPresenter(screen, navigator, repository, reconciler)

            presenter.test {
                awaitItem().eventSink(CloudBackupUiState.Event.RestoreBackup)

                assertEquals(HomeScreen, navigator.awaitResetRoot().newRoot)
                assertEquals(1, repository.restoreCalls)
                assertEquals(1, reconciler.reconcileCalls)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun startupCanContinueToConfiguredFallback() =
        runTest {
            val screen =
                CloudBackupScreen(
                    entryPoint = CloudBackupScreen.EntryPoint.STARTUP,
                    fallback = CloudBackupScreen.Fallback.ONBOARDING,
                )
            val navigator = FakeNavigator(screen)
            val presenter = CloudBackupPresenter(screen, navigator, FakeCloudBackupRepository())

            presenter.test {
                awaitItem().eventSink(CloudBackupUiState.Event.StartFresh)

                assertEquals(OnboardingScreen, navigator.awaitResetRoot().newRoot)
            }
        }

    private fun presenter(
        entryPoint: CloudBackupScreen.EntryPoint,
        repository: CloudBackupRepository,
    ): CloudBackupPresenter {
        val screen = CloudBackupScreen(entryPoint = entryPoint)
        return CloudBackupPresenter(screen, FakeNavigator(screen), repository)
    }
}

private class RecordingReconciler : DeadlineReminderReconciler {
    override val schedulingHealth: StateFlow<DeadlineReminderSchedulingHealth> =
        MutableStateFlow(DeadlineReminderSchedulingHealth())
    var reconcileCalls = 0

    override suspend fun reconcileAll() {
        reconcileCalls += 1
    }
}

private class FakeCloudBackupRepository(
    private var existing: BackupMetadata? = null,
) : CloudBackupRepository {
    override val isSupported = true
    var createCalls = 0
    var restoreCalls = 0

    override suspend fun hasLocalData(): Boolean = true

    override suspend fun findBackup(): BackupMetadata? = existing

    override suspend fun createBackup(): BackupMetadata =
        BackupMetadata(3, "created").also {
            createCalls += 1
            existing = it
        }

    override suspend fun restoreBackup(): BackupMetadata? {
        restoreCalls += 1
        return existing
    }
}
