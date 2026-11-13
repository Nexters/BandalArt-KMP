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
import com.nexters.bandalart.core.domain.entity.WeeklyReviewSummary
import com.nexters.bandalart.core.domain.notification.BufferedWeeklyReviewLaunchTarget
import com.nexters.bandalart.core.domain.notification.DeadlineNotificationAuthorization
import com.nexters.bandalart.core.domain.notification.DeadlineNotificationAuthorizationStatus
import com.nexters.bandalart.core.domain.notification.WeeklyReviewNotificationAuthorization
import com.nexters.bandalart.core.domain.notification.WeeklyReviewReminderReconciler
import com.nexters.bandalart.feature.home.HomeScreen
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomePresenterWeeklyReviewTest {
    @Test
    fun grantedPermissionEnablesWeeklyReminderOnlyAndReconciles() =
        runTest {
            val settings = FakeSettingsRepository()
            val reconciler = RecordingWeeklyReconciler()
            val presenter =
                presenter(
                    settings = settings,
                    authorization = FakeAuthorization(DeadlineNotificationAuthorizationStatus.GRANTED),
                    reconciler = reconciler,
                )

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData == null) state = awaitItem()
                state.eventSink(HomeScreen.Event.ConfirmWeeklyReviewReminderPermission)
                while (!state.weeklyReviewReminderEnabled) state = awaitItem()

                assertFalse(state.deadlineReminderEnabled)
                assertTrue(reconciler.reconcileCalls > 0)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun platformPermissionResultIsAppliedToWeeklyReminder() =
        runTest {
            val authorization = FakeAuthorization(DeadlineNotificationAuthorizationStatus.REQUESTABLE)
            val presenter = presenter(authorization = authorization)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData == null) state = awaitItem()
                state.eventSink(HomeScreen.Event.ConfirmWeeklyReviewReminderPermission)
                while (state.deadlinePermissionRequestId == null) state = awaitItem()

                authorization.status = DeadlineNotificationAuthorizationStatus.GRANTED
                state.eventSink(HomeScreen.Event.DeadlineReminderPermissionResult)
                while (!state.weeklyReviewReminderEnabled) state = awaitItem()

                assertFalse(state.deadlineReminderEnabled)
                assertEquals(null, state.deadlinePermissionRequestId)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun blockedEnableIntentCompletesAfterGrantingPermissionInSystemSettings() =
        runTest {
            val authorization = FakeAuthorization(DeadlineNotificationAuthorizationStatus.BLOCKED)
            val presenter = presenter(authorization = authorization)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData == null) state = awaitItem()
                state.eventSink(HomeScreen.Event.ConfirmWeeklyReviewReminderPermission)
                advanceUntilIdle()
                assertEquals(1, authorization.openSettingsCalls)

                authorization.status = DeadlineNotificationAuthorizationStatus.GRANTED
                state.eventSink(HomeScreen.Event.DeadlineReminderForegrounded)
                while (!state.weeklyReviewReminderEnabled) state = awaitItem()

                assertFalse(state.deadlineReminderEnabled)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun weeklyChannelBlockedIsTrackedSeparatelyFromDeadlineChannel() =
        runTest {
            val deadlineAuthorization = FakeAuthorization(DeadlineNotificationAuthorizationStatus.GRANTED)
            val weeklyAuthorization = FakeAuthorization(DeadlineNotificationAuthorizationStatus.BLOCKED)
            val presenter =
                presenter(
                    settings = FakeSettingsRepository(initialWeeklyReviewReminderEnabled = true),
                    authorization = weeklyAuthorization,
                    deadlineAuthorization = deadlineAuthorization,
                )

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData == null) state = awaitItem()
                state.eventSink(HomeScreen.Event.OpenSettings)
                while (
                    state.weeklyReviewNotificationAuthorizationStatus != DeadlineNotificationAuthorizationStatus.BLOCKED ||
                    state.deadlineNotificationAuthorizationStatus != DeadlineNotificationAuthorizationStatus.GRANTED
                ) {
                    state = awaitItem()
                }

                state.eventSink(HomeScreen.Event.ConfirmWeeklyReviewReminderPermission)
                advanceUntilIdle()
                assertEquals(1, weeklyAuthorization.openSettingsCalls)
                assertEquals(0, deadlineAuthorization.openSettingsCalls)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun turningOffWeeklyReminderSavesAndReconciles() =
        runTest {
            val reconciler = RecordingWeeklyReconciler()
            val presenter =
                presenter(
                    settings = FakeSettingsRepository(initialWeeklyReviewReminderEnabled = true),
                    reconciler = reconciler,
                )

            presenter.test {
                var state = awaitItem()
                while (!state.weeklyReviewReminderEnabled || state.bandalartData == null) state = awaitItem()
                state.eventSink(HomeScreen.Event.SetWeeklyReviewReminderEnabled(false))
                while (state.weeklyReviewReminderEnabled) state = awaitItem()

                advanceUntilIdle()
                assertTrue(reconciler.reconcileCalls > 0)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun notificationLaunchOpensWeeklyReviewSheet() =
        runTest {
            val launchTarget = BufferedWeeklyReviewLaunchTarget().apply { record() }
            val presenter = presenter(launchTarget = launchTarget)

            presenter.test {
                var state = awaitItem()
                while (state.bottomSheet !is HomeScreen.BottomSheetState.WeeklyReview) state = awaitItem()

                assertEquals(summary, (state.bottomSheet as HomeScreen.BottomSheetState.WeeklyReview).summary)
                assertFalse(launchTarget.pending.value)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun selectingBandalartFromWeeklyReviewClosesSheetAndSelectsIt() =
        runTest {
            val repository = repository()
            val presenter = presenter(repository = repository)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData?.id != 1L) state = awaitItem()
                state.eventSink(HomeScreen.Event.OpenWeeklyReview)
                while (state.bottomSheet !is HomeScreen.BottomSheetState.WeeklyReview) state = awaitItem()

                state.eventSink(HomeScreen.Event.SelectWeeklyReviewBandalart(2))
                while (state.bandalartData?.id != 2L) state = awaitItem()

                assertEquals(null, state.bottomSheet)
                assertEquals(2L, repository.recentBandalartId)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun firstStreakShowsReminderNudgeOnceAndRecordsFlag() =
        runTest {
            val repository = repository()
            val settings = FakeSettingsRepository()
            val presenter = presenter(repository = repository, settings = settings)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData == null) state = awaitItem()
                repository.completionStreak.value = 1
                while (state.dialog != HomeScreen.DialogState.WeeklyReviewReminderNudge) state = awaitItem()

                state.eventSink(HomeScreen.Event.DismissDialog)
                while (state.dialog != null) state = awaitItem()
                repository.completionStreak.value = 2
                advanceUntilIdle()

                assertEquals(null, expectMostRecentItem().dialog)
                assertEquals(1, settings.weeklyReviewReminderNudgeMarks)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun reminderNudgeIsNotShownAgainAfterFlagIsRecorded() =
        runTest {
            val repository = repository()
            val settings = FakeSettingsRepository(initialWeeklyReviewReminderNudgeShown = true)
            val presenter = presenter(repository = repository, settings = settings)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData == null) state = awaitItem()
                repository.completionStreak.value = 1
                while (state.completionStreak != 1) state = awaitItem()
                advanceUntilIdle()

                assertEquals(0, settings.weeklyReviewReminderNudgeMarks)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun enabledReminderRecordsNudgeFlagWithoutDialog() =
        runTest {
            val repository = repository()
            val settings = FakeSettingsRepository(initialWeeklyReviewReminderEnabled = true)
            val presenter = presenter(repository = repository, settings = settings)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData == null) state = awaitItem()
                repository.completionStreak.value = 1
                while (state.completionStreak != 1) state = awaitItem()
                advanceUntilIdle()

                assertEquals(1, settings.weeklyReviewReminderNudgeMarks)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun confirmingReminderNudgeClosesDialogAndEnablesReminder() =
        runTest {
            val repository = repository()
            val presenter =
                presenter(
                    repository = repository,
                    authorization = FakeAuthorization(DeadlineNotificationAuthorizationStatus.GRANTED),
                )

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData == null) state = awaitItem()
                repository.completionStreak.value = 1
                while (state.dialog != HomeScreen.DialogState.WeeklyReviewReminderNudge) state = awaitItem()

                state.eventSink(HomeScreen.Event.ConfirmWeeklyReviewReminderPermission)
                while (!state.weeklyReviewReminderEnabled) state = awaitItem()

                assertEquals(null, state.dialog)
                assertFalse(state.deadlineReminderEnabled)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun reminderNudgeWaitsUntilBottomSheetIsClosed() =
        runTest {
            val repository = repository()
            val settings = FakeSettingsRepository()
            val presenter = presenter(repository = repository, settings = settings)

            presenter.test {
                var state = awaitItem()
                while (state.bandalartData == null) state = awaitItem()
                state.eventSink(HomeScreen.Event.OpenSettings)
                while (state.bottomSheet != HomeScreen.BottomSheetState.Settings) state = awaitItem()
                repository.completionStreak.value = 1
                while (state.completionStreak != 1) state = awaitItem()
                advanceUntilIdle()

                assertEquals(0, settings.weeklyReviewReminderNudgeMarks)

                state.eventSink(HomeScreen.Event.DismissBottomSheet)
                while (state.dialog != HomeScreen.DialogState.WeeklyReviewReminderNudge) state = awaitItem()

                assertEquals(1, settings.weeklyReviewReminderNudgeMarks)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private val summary =
        WeeklyReviewSummary(
            weekStart = LocalDate(2026, 3, 2),
            weekEnd = LocalDate(2026, 3, 4),
            completedCount = 0,
            activeDayCount = 0,
            bandalarts = emptyList(),
        )

    private fun presenter(
        repository: FakeBandalartRepository = repository(),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
        authorization: DeadlineNotificationAuthorization =
            FakeAuthorization(DeadlineNotificationAuthorizationStatus.REQUESTABLE),
        deadlineAuthorization: DeadlineNotificationAuthorization =
            FakeAuthorization(DeadlineNotificationAuthorizationStatus.REQUESTABLE),
        reconciler: WeeklyReviewReminderReconciler = RecordingWeeklyReconciler(),
        launchTarget: BufferedWeeklyReviewLaunchTarget = BufferedWeeklyReviewLaunchTarget(),
    ) = HomePresenter(
        navigator = FakeNavigator(HomeScreen),
        bandalartRepository = repository,
        bandalartSlotRepository = FakeBandalartSlotRepository(),
        inAppUpdateRepository = FakeInAppUpdateRepository(),
        settingsRepository = settings,
        deadlineNotificationAuthorization = deadlineAuthorization,
        weeklyReviewNotificationAuthorization = WeeklyReviewNotificationAuthorization(authorization),
        weeklyReviewReminderReconciler = reconciler,
        weeklyReviewLaunchTarget = launchTarget,
    )

    private fun repository() =
        FakeBandalartRepository(
            initialBandalarts = listOf(bandalart(1), bandalart(2)),
            recentBandalartId = 1,
            weeklyReviewSummary = summary,
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

    private class FakeAuthorization(
        var status: DeadlineNotificationAuthorizationStatus,
    ) : DeadlineNotificationAuthorization {
        var openSettingsCalls = 0

        override suspend fun getStatus() = status

        override suspend fun requestAuthorization() = status

        override suspend fun openSettings() {
            openSettingsCalls += 1
        }
    }

    private class RecordingWeeklyReconciler : WeeklyReviewReminderReconciler {
        var reconcileCalls = 0

        override suspend fun reconcile() {
            reconcileCalls += 1
        }
    }
}
