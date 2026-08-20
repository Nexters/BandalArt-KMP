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

package com.nexters.bandalart.feature.home.presenter

import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.notification.BufferedDeadlineNotificationLaunchTarget
import com.nexters.bandalart.core.domain.notification.DeadlineNotificationAuthorization
import com.nexters.bandalart.core.domain.notification.DeadlineNotificationAuthorizationStatus
import com.nexters.bandalart.core.domain.notification.DeadlineReminderReconciler
import com.nexters.bandalart.core.domain.notification.DeadlineReminderScheduler
import com.nexters.bandalart.core.domain.notification.DeadlineReminderSchedulingHealth
import com.nexters.bandalart.core.domain.notification.DeadlineReminderSchedulingErrorCategory
import com.nexters.bandalart.core.domain.notification.DeadlineReminderSchedulingResult
import com.nexters.bandalart.core.domain.notification.DeadlineReminderBatch
import com.nexters.bandalart.feature.home.HomeScreen
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomePresenterDeadlineReminderTest {
    @Test
    fun notificationLaunchSelectsBufferedBandalartAfterHomeIsReady() =
        runTest {
            val launchTarget = BufferedDeadlineNotificationLaunchTarget().apply { record(2) }
            val repository = repository()
            val presenter = presenter(repository, launchTarget = launchTarget)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData?.id != 2L) state = awaitItem()

                assertEquals(2L, repository.recentBandalartId)
                assertEquals(null, launchTarget.pendingBandalartId.value)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun warmNotificationLaunchSelectsAndConsumesBandalart() =
        runTest {
            val launchTarget = BufferedDeadlineNotificationLaunchTarget()
            val repository = repository()
            val presenter = presenter(repository, launchTarget = launchTarget)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData == null) state = awaitItem()

                launchTarget.record(2)
                while (state.bandalartData?.id != 2L) state = awaitItem()

                assertEquals(2L, repository.recentBandalartId)
                assertEquals(null, launchTarget.pendingBandalartId.value)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun notificationTargetCancelsDelayedInitialSelectionAndWins() =
        runTest {
            val initialLoadStarted = CompletableDeferred<Unit>()
            val releaseInitialLoad = CompletableDeferred<Unit>()
            val launchTarget = BufferedDeadlineNotificationLaunchTarget()
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1), bandalart(2)),
                    recentBandalartId = 1,
                    beforeBandalartLoad = { id ->
                        if (id == 1L) {
                            initialLoadStarted.complete(Unit)
                            releaseInitialLoad.await()
                        }
                    },
                )
            val presenter = presenter(repository, launchTarget = launchTarget)

            presenter.test {
                var state = awaitItem()
                initialLoadStarted.await()

                launchTarget.record(2)
                while (state.bandalartData?.id != 2L) state = awaitItem()

                assertEquals(2L, repository.recentBandalartId)
                assertEquals(null, launchTarget.pendingBandalartId.value)
                releaseInitialLoad.complete(Unit)
                advanceUntilIdle()
                state = expectMostRecentItem()
                assertEquals(2L, state.bandalartData?.id)
                assertEquals(2L, repository.recentBandalartId)
                assertEquals(null, launchTarget.pendingBandalartId.value)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun notificationTargetWinsDelayedManualSelection() =
        runTest {
            val manualLoadStarted = CompletableDeferred<Unit>()
            val releaseManualLoad = CompletableDeferred<Unit>()
            val launchTarget = BufferedDeadlineNotificationLaunchTarget()
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1), bandalart(2)),
                    recentBandalartId = 1,
                    mainCells = mapOf(1L to mainCell(101, "one"), 2L to mainCell(201, "two")),
                    beforeBandalartLoad = { id ->
                        if (id == 2L) {
                            manualLoadStarted.complete(Unit)
                            releaseManualLoad.await()
                        }
                    },
                )
            val presenter = presenter(repository, launchTarget = launchTarget)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData?.id != 1L) state = awaitItem()

                state.eventSink(HomeScreen.Event.SelectBandalart(2))
                manualLoadStarted.await()
                launchTarget.record(1)
                while (
                    launchTarget.pendingBandalartId.value != null ||
                    repository.recentBandalartId != 1L
                ) {
                    state = awaitItem()
                }

                assertEquals(1L, state.bandalartData?.id)
                assertEquals(101L, state.bandalartCellData?.id)
                assertEquals(1L, repository.recentBandalartId)
                assertEquals(null, launchTarget.pendingBandalartId.value)

                releaseManualLoad.complete(Unit)
                advanceUntilIdle()
                state = expectMostRecentItem()
                assertEquals(1L, state.bandalartData?.id)
                assertEquals(101L, state.bandalartCellData?.id)
                assertEquals(1L, repository.recentBandalartId)
                assertEquals(null, launchTarget.pendingBandalartId.value)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun notificationTargetWinsDelayedCompletionRefreshWithoutMixingCellTree() =
        runTest {
            var boardOneLoadCount = 0
            val completionLoadStarted = CompletableDeferred<Unit>()
            val releaseCompletionLoad = CompletableDeferred<Unit>()
            val launchTarget = BufferedDeadlineNotificationLaunchTarget()
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1), bandalart(2)),
                    recentBandalartId = 1,
                    mainCells = mapOf(1L to mainCell(101, "one"), 2L to mainCell(201, "two")),
                    beforeBandalartLoad = { id ->
                        if (id == 1L && ++boardOneLoadCount == 2) {
                            completionLoadStarted.complete(Unit)
                            releaseCompletionLoad.await()
                        }
                    },
                )
            val presenter = presenter(repository, launchTarget = launchTarget)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData?.id != 1L) state = awaitItem()
                repository.publishBandalartRevision(bandalart(1).copy(isCompleted = true))
                completionLoadStarted.await()

                launchTarget.record(2)
                while (state.bandalartData?.id != 2L) state = awaitItem()

                assertEquals(201L, state.bandalartCellData?.id)
                assertEquals(null, launchTarget.pendingBandalartId.value)
                releaseCompletionLoad.complete(Unit)
                advanceUntilIdle()
                state = expectMostRecentItem()
                assertEquals(2L, state.bandalartData?.id)
                assertEquals(201L, state.bandalartCellData?.id)
                assertEquals(null, launchTarget.pendingBandalartId.value)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun listRevisionRefreshesCurrentBoardAndCellTree() =
        runTest {
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1), bandalart(2)),
                    recentBandalartId = 1,
                    mainCells = mapOf(1L to mainCell(101, "before"), 2L to mainCell(201, "two")),
                )
            val presenter = presenter(repository)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData?.id != 1L) state = awaitItem()

                repository.publishBandalartRevision(
                    bandalart = bandalart(1).copy(title = "edited"),
                    mainCell = mainCell(101, "edited"),
                )
                while (
                    state.bandalartData?.titleText != "edited" ||
                    state.bandalartCellData?.title != "edited"
                ) {
                    state = awaitItem()
                }

                assertEquals(1L, state.bandalartData.id)
                assertEquals(101L, state.bandalartCellData.id)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun deletingCurrentBoardSelectsRemainingBoardAndUpdatesRecentId() =
        runTest {
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(1), bandalart(2)),
                    recentBandalartId = 1,
                    mainCells = mapOf(1L to mainCell(101, "one"), 2L to mainCell(201, "two")),
                )
            val presenter = presenter(repository)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData?.id != 1L) state = awaitItem()

                state.eventSink(HomeScreen.Event.DeleteBandalart(1))
                while (state.bandalartData?.id != 2L) state = awaitItem()

                assertEquals(2L, repository.recentBandalartId)
                assertEquals(201L, state.bandalartCellData?.id)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun invalidNotificationLaunchIsExplicitlyConsumedWithoutChangingSelection() =
        runTest {
            val launchTarget = BufferedDeadlineNotificationLaunchTarget().apply { record(999) }
            val repository = repository()
            val presenter = presenter(repository, launchTarget = launchTarget)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData == null) state = awaitItem()
                while (launchTarget.pendingBandalartId.value != null) state = awaitItem()

                assertEquals(1L, state.bandalartData?.id)
                assertEquals(1L, repository.recentBandalartId)
            }
        }

    @Test
    fun grantedPermissionPersistsPreferenceAndReconciles() =
        runTest {
            val settings = FakeSettingsRepository()
            val authorization = FakeAuthorization(DeadlineNotificationAuthorizationStatus.GRANTED)
            val reconciler = RecordingReconciler()
            val presenter =
                presenter(
                    repository = repository(),
                    settings = settings,
                    authorization = authorization,
                    reconciler = reconciler,
                )

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData == null) state = awaitItem()
                state.eventSink(HomeScreen.Event.OpenSettings)
                state.eventSink(HomeScreen.Event.ConfirmDeadlineReminderPermission)
                do {
                    state = awaitItem()
                } while (!state.deadlineReminderEnabled)

                assertTrue(reconciler.reconcileCalls > 0)
            }
        }

    @Test
    fun requestableAuthorizationThatBecomesQuietEnablesAndReconcilesWithoutPlatformEffect() =
        runTest {
            val settings = FakeSettingsRepository()
            val authorization =
                FakeAuthorization(
                    status = DeadlineNotificationAuthorizationStatus.REQUESTABLE,
                    requestedStatus = DeadlineNotificationAuthorizationStatus.QUIET,
                )
            val reconciler = RecordingReconciler()
            val presenter =
                presenter(
                    repository = repository(),
                    settings = settings,
                    authorization = authorization,
                    reconciler = reconciler,
                )

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData == null) state = awaitItem()
                state.eventSink(HomeScreen.Event.OpenSettings)
                state.eventSink(HomeScreen.Event.ConfirmDeadlineReminderPermission)
                while (!state.deadlineReminderEnabled) state = awaitItem()

                assertEquals(1, authorization.requestCalls)
                assertEquals(DeadlineNotificationAuthorizationStatus.QUIET, state.deadlineNotificationAuthorizationStatus)
                assertEquals(null, state.deadlinePermissionRequestId)
                assertTrue(reconciler.reconcileCalls > 0)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun blockedEnabledReminderCanBeTurnedOff() =
        runTest {
            val settings = FakeSettingsRepository(initialDeadlineReminderEnabled = true)
            val presenter =
                presenter(
                    repository = repository(),
                    settings = settings,
                    authorization = FakeAuthorization(DeadlineNotificationAuthorizationStatus.BLOCKED),
                )

            presenter.test {
                var state = awaitItem()
                while (!state.deadlineReminderEnabled) state = awaitItem()
                state.eventSink(HomeScreen.Event.SetDeadlineReminderEnabled(false))
                while (state.deadlineReminderEnabled) state = awaitItem()

                assertEquals(false, state.deadlineReminderEnabled)
            }
        }

    @Test
    fun foregroundAfterSystemSettingsRefreshesGrantAndReconcilesEnabledPreference() =
        runTest {
            val settings = FakeSettingsRepository(initialDeadlineReminderEnabled = true)
            val authorization = FakeAuthorization(DeadlineNotificationAuthorizationStatus.BLOCKED)
            val reconciler = RecordingReconciler()
            val presenter =
                presenter(
                    repository = repository(),
                    settings = settings,
                    authorization = authorization,
                    reconciler = reconciler,
                )

            presenter.test {
                var state = awaitItem()
                state.eventSink(HomeScreen.Event.OpenSettings)
                while (
                    state.deadlineNotificationAuthorizationStatus !=
                    DeadlineNotificationAuthorizationStatus.BLOCKED
                ) {
                    state = awaitItem()
                }
                authorization.status = DeadlineNotificationAuthorizationStatus.GRANTED
                state.eventSink(HomeScreen.Event.DeadlineReminderForegrounded)
                while (
                    state.deadlineNotificationAuthorizationStatus !=
                    DeadlineNotificationAuthorizationStatus.GRANTED
                ) {
                    state = awaitItem()
                }

                assertTrue(reconciler.reconcileCalls > 0)
            }
        }

    @Test
    fun foregroundReconcilesEvenWhenReminderPreferenceIsDisabled() =
        runTest {
            val authorization = FakeAuthorization(DeadlineNotificationAuthorizationStatus.GRANTED)
            val reconciler = RecordingReconciler()
            val presenter =
                presenter(
                    repository = repository(),
                    authorization = authorization,
                    reconciler = reconciler,
                )

            presenter.test {
                var state = awaitItem()
                state.eventSink(HomeScreen.Event.DeadlineReminderForegrounded)
                while (
                    state.deadlineNotificationAuthorizationStatus !=
                    DeadlineNotificationAuthorizationStatus.GRANTED
                ) {
                    state = awaitItem()
                }

                assertEquals(false, state.deadlineReminderEnabled)
                assertEquals(1, reconciler.reconcileCalls)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun blockedEnableIntentCompletesAfterGrantingPermissionInSystemSettings() =
        runTest {
            val settings = FakeSettingsRepository()
            val authorization = FakeAuthorization(DeadlineNotificationAuthorizationStatus.BLOCKED)
            val reconciler = RecordingReconciler()
            val presenter =
                presenter(
                    repository = repository(),
                    settings = settings,
                    authorization = authorization,
                    reconciler = reconciler,
                )

            presenter.test {
                var state = awaitItem()
                state.eventSink(HomeScreen.Event.OpenSettings)
                while (
                    state.deadlineNotificationAuthorizationStatus !=
                    DeadlineNotificationAuthorizationStatus.BLOCKED
                ) {
                    state = awaitItem()
                }
                state.eventSink(HomeScreen.Event.ConfirmDeadlineReminderPermission)
                advanceUntilIdle()
                assertEquals(1, authorization.openSettingsCalls)

                authorization.status = DeadlineNotificationAuthorizationStatus.GRANTED
                state.eventSink(HomeScreen.Event.DeadlineReminderForegrounded)
                while (!state.deadlineReminderEnabled) state = awaitItem()

                assertEquals(DeadlineNotificationAuthorizationStatus.GRANTED, state.deadlineNotificationAuthorizationStatus)
                assertEquals(1, reconciler.reconcileCalls)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun sendingTestNotificationDelegatesToSchedulerAndReportsSuccess() =
        runTest {
            val scheduler = RecordingScheduler()
            val presenter = presenter(repository = repository(), scheduler = scheduler)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData == null) state = awaitItem()

                state.eventSink(HomeScreen.Event.SendDeadlineReminderTestNotification)
                while (state.effect != HomeScreen.Effect.ShowDeadlineReminderTestSentSnackbar) {
                    state = awaitItem()
                }

                assertEquals(1, scheduler.testNotificationCalls)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun failedTestNotificationReportsFailure() =
        runTest {
            val scheduler =
                RecordingScheduler(
                    DeadlineReminderSchedulingResult(
                        scheduledCount = 0,
                        lastErrorCategory = DeadlineReminderSchedulingErrorCategory.SCHEDULING,
                    ),
                )
            val presenter = presenter(repository = repository(), scheduler = scheduler)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData == null) state = awaitItem()

                state.eventSink(HomeScreen.Event.SendDeadlineReminderTestNotification)
                while (state.effect != HomeScreen.Effect.ShowDeadlineReminderTestFailedSnackbar) {
                    state = awaitItem()
                }

                assertEquals(1, scheduler.testNotificationCalls)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun presenter(
        repository: FakeBandalartRepository,
        settings: FakeSettingsRepository = FakeSettingsRepository(),
        authorization: DeadlineNotificationAuthorization =
            FakeAuthorization(DeadlineNotificationAuthorizationStatus.REQUESTABLE),
        reconciler: DeadlineReminderReconciler = RecordingReconciler(),
        scheduler: DeadlineReminderScheduler = RecordingScheduler(),
        launchTarget: BufferedDeadlineNotificationLaunchTarget = BufferedDeadlineNotificationLaunchTarget(),
    ) = HomePresenter(
        navigator = FakeNavigator(HomeScreen),
        bandalartRepository = repository,
        bandalartSlotRepository = FakeBandalartSlotRepository(),
        inAppUpdateRepository = FakeInAppUpdateRepository(),
        settingsRepository = settings,
        deadlineNotificationAuthorization = authorization,
        deadlineReminderReconciler = reconciler,
        deadlineReminderScheduler = scheduler,
        deadlineNotificationLaunchTarget = launchTarget,
    )

    private fun repository() =
        FakeBandalartRepository(
            initialBandalarts = listOf(bandalart(1), bandalart(2)),
            recentBandalartId = 1,
        )

    private fun bandalart(id: Long) =
        BandalartEntity(
            id = id,
            mainColor = "#3FFFBA",
            subColor = "#111827",
            profileEmoji = "🎯",
            title = "반다라트 $id",
            description = null,
            dueDate = null,
            isCompleted = false,
            completionRatio = 0,
        )

    private fun mainCell(
        id: Long,
        title: String,
    ) = BandalartCellEntity(
        id = id,
        title = title,
        parentId = null,
    )

    private class FakeAuthorization(
        var status: DeadlineNotificationAuthorizationStatus,
        private val requestedStatus: DeadlineNotificationAuthorizationStatus = status,
    ) : DeadlineNotificationAuthorization {
        var requestCalls = 0
        var openSettingsCalls = 0

        override suspend fun getStatus() = status

        override suspend fun requestAuthorization(): DeadlineNotificationAuthorizationStatus {
            requestCalls += 1
            status = requestedStatus
            return status
        }

        override suspend fun openSettings() {
            openSettingsCalls += 1
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

    private class RecordingScheduler(
        private val testResult: DeadlineReminderSchedulingResult = DeadlineReminderSchedulingResult(scheduledCount = 1),
    ) : DeadlineReminderScheduler {
        var testNotificationCalls = 0

        override suspend fun replaceAll(batches: List<DeadlineReminderBatch>) = DeadlineReminderSchedulingResult(scheduledCount = batches.size)

        override suspend fun clearAll() = DeadlineReminderSchedulingResult(scheduledCount = 0)

        override suspend fun postTestNotification(): DeadlineReminderSchedulingResult {
            testNotificationCalls += 1
            return testResult
        }
    }
}
