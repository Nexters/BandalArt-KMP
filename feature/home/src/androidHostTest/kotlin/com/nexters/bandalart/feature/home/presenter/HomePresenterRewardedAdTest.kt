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

import app.cash.turbine.ReceiveTurbine
import com.nexters.bandalart.core.common.RewardedAdResult
import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.core.domain.repository.PendingRewardedCreation
import com.nexters.bandalart.core.domain.template.BandalartTemplateId
import com.nexters.bandalart.feature.home.HomeScreen
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HomePresenterRewardedAdTest {
    @Test
    fun templateUsesExistingFreeSlotGateAndCreatesSelectedTemplate() =
        runTest {
            val repository = repository()
            val slotRepository = FakeBandalartSlotRepository(maxSlots = 4)

            presenter(repository, slotRepository).test {
                var state = awaitLoaded()
                state.eventSink(
                    HomeScreen.Event.CreateBandalartFromTemplate(
                        BandalartTemplateId.STUDY_PLAN_V1,
                    ),
                )
                awaitCreated()

                assertEquals(listOf(BandalartTemplateId.STUDY_PLAN_V1), repository.createdTemplateIds)
                assertEquals(0, slotRepository.expandCalls)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun slotLookupFailureDoesNotBypassGate() =
        runTest {
            val repository = repository()
            val slotRepository =
                FakeBandalartSlotRepository(
                    maxSlots = 3,
                    getError = IllegalStateException("read failed"),
                )

            presenter(repository, slotRepository).test {
                var state = awaitLoaded()
                state.eventSink(HomeScreen.Event.AddBandalart)
                do {
                    state = awaitItem()
                } while (state.effect != HomeScreen.Effect.ShowSlotErrorSnackbar)

                assertEquals(0, repository.createCalls)
                assertEquals(0, slotRepository.expandCalls)
                assertNull(state.dialog)
            }
        }

    @Test
    fun usedSlotsShowDialogBeforeRequestingRewardedAd() =
        runTest {
            val repository = repository()
            val slotRepository = FakeBandalartSlotRepository(maxSlots = 3)

            presenter(repository, slotRepository).test {
                var state = awaitLoaded()
                state.eventSink(HomeScreen.Event.AddBandalart)
                state = awaitDialog()

                assertEquals(HomeScreen.DialogState.RewardedCreate, state.dialog)
                assertNull(state.effect)
                assertEquals(0, repository.createCalls)
                assertEquals(0, slotRepository.expandCalls)
            }
        }

    @Test
    fun cancelingDialogDoesNotCreateBandalart() =
        runTest {
            val repository = repository()
            val slotRepository = FakeBandalartSlotRepository(maxSlots = 3)

            presenter(repository, slotRepository).test {
                var state = awaitLoaded()
                state.eventSink(HomeScreen.Event.AddBandalart)
                state = awaitDialog()
                state.eventSink(HomeScreen.Event.DismissDialog)
                do {
                    state = awaitItem()
                } while (state.dialog != null)

                assertEquals(0, repository.createCalls)
                assertEquals(0, slotRepository.expandCalls)
            }
        }

    @Test
    fun rewardExpandsSlotAndCreatesExactlyOnce() =
        runTest {
            val repository = repository()
            val slotRepository = FakeBandalartSlotRepository(maxSlots = 3)

            presenter(repository, slotRepository).test {
                var state = awaitLoaded()
                state.eventSink(HomeScreen.Event.AddBandalart)
                state = awaitDialog()
                state.eventSink(HomeScreen.Event.ConfirmRewardedCreate)
                val requestId = awaitRewardedAdRequest()

                state.eventSink(
                    HomeScreen.Event.RewardedAdFinished(
                        requestId = requestId,
                        result = RewardedAdResult.REWARDED,
                    ),
                )
                state = awaitCreated()
                state.eventSink(
                    HomeScreen.Event.RewardedAdFinished(
                        requestId = requestId,
                        result = RewardedAdResult.REWARDED,
                    ),
                )

                assertEquals(1, slotRepository.expandCalls)
                assertEquals(1, repository.createCalls)
                assertEquals(4L, repository.recentBandalartId)
            }
        }

    @Test
    fun rewardedCreationPersistsSelectedTemplateUntilGrant() =
        runTest {
            val repository = repository()
            val slotRepository = FakeBandalartSlotRepository(maxSlots = 3)

            presenter(repository, slotRepository).test {
                var state = awaitLoaded()
                state.eventSink(
                    HomeScreen.Event.CreateBandalartFromTemplate(
                        BandalartTemplateId.JOB_PREPARATION_V1,
                    ),
                )
                state = awaitDialog()
                state.eventSink(HomeScreen.Event.ConfirmRewardedCreate)
                val requestId = awaitRewardedAdRequest()

                assertEquals(
                    BandalartTemplateId.JOB_PREPARATION_V1,
                    slotRepository.pendingRewardedCreation?.templateId,
                )
                state.eventSink(
                    HomeScreen.Event.RewardedAdFinished(
                        requestId = requestId,
                        result = RewardedAdResult.REWARDED,
                    ),
                )
                awaitCreated()

                assertEquals(listOf(BandalartTemplateId.JOB_PREPARATION_V1), repository.createdTemplateIds)
            }
        }

    @Test
    fun loadOrShowFailureFailsOpenButDismissDoesNotCreate() =
        runTest {
            val failedRepository = repository()
            val failedSlots = FakeBandalartSlotRepository(maxSlots = 3)

            presenter(failedRepository, failedSlots).test {
                var state = awaitLoaded()
                state.eventSink(
                    HomeScreen.Event.CreateBandalartFromTemplate(
                        BandalartTemplateId.MONEY_HABIT_V1,
                    ),
                )
                state = awaitDialog()
                state.eventSink(HomeScreen.Event.ConfirmRewardedCreate)
                val requestId = awaitRewardedAdRequest()
                state.eventSink(
                    HomeScreen.Event.RewardedAdFinished(requestId, RewardedAdResult.FAILED),
                )
                do {
                    state = awaitItem()
                } while (state.effect != HomeScreen.Effect.ShowAdUnavailableSnackbar)
                state.eventSink(HomeScreen.Event.ConsumeEffect)
                awaitCreated()

                assertEquals(1, failedSlots.expandCalls)
                assertEquals(1, failedRepository.createCalls)
                assertEquals(listOf(BandalartTemplateId.MONEY_HABIT_V1), failedRepository.createdTemplateIds)
            }

            val dismissedRepository = repository()
            val dismissedSlots = FakeBandalartSlotRepository(maxSlots = 3)

            presenter(dismissedRepository, dismissedSlots).test {
                var state = awaitLoaded()
                state.eventSink(HomeScreen.Event.AddBandalart)
                state = awaitDialog()
                state.eventSink(HomeScreen.Event.ConfirmRewardedCreate)
                val requestId = awaitRewardedAdRequest()
                state.eventSink(
                    HomeScreen.Event.RewardedAdFinished(requestId, RewardedAdResult.DISMISSED),
                )
                do {
                    state = awaitItem()
                } while (state.effect != null)

                assertEquals(0, dismissedSlots.expandCalls)
                assertEquals(0, dismissedRepository.createCalls)
            }
        }

    @Test
    fun slotExpansionFailureDoesNotCreateBandalart() =
        runTest {
            val repository = repository()
            val slotRepository =
                FakeBandalartSlotRepository(
                    maxSlots = 3,
                    expandError = IllegalStateException("write failed"),
                )

            presenter(repository, slotRepository).test {
                var state = awaitLoaded()
                state.eventSink(HomeScreen.Event.AddBandalart)
                state = awaitDialog()
                state.eventSink(HomeScreen.Event.ConfirmRewardedCreate)
                val requestId = awaitRewardedAdRequest()
                state.eventSink(
                    HomeScreen.Event.RewardedAdFinished(requestId, RewardedAdResult.REWARDED),
                )
                do {
                    state = awaitItem()
                } while (state.effect != HomeScreen.Effect.ShowSlotErrorSnackbar)

                assertEquals(1, slotRepository.expandCalls)
                assertEquals(0, repository.createCalls)
            }
        }

    @Test
    fun repeatedAddIsBlockedUntilCreatedBandalartIsObserved() =
        runTest {
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = List(3) { index -> bandalart(index + 1L) },
                    recentBandalartId = 1L,
                    createdBandalart = bandalart(4L),
                    publishCreatedBandalartImmediately = false,
                )
            val slotRepository = FakeBandalartSlotRepository(maxSlots = 4)

            presenter(repository, slotRepository).test {
                var state = awaitLoaded()
                state.eventSink(HomeScreen.Event.AddBandalart)
                state = awaitCreated()
                state.eventSink(HomeScreen.Event.AddBandalart)

                assertEquals(1, repository.createCalls)
                repository.publishCreatedBandalart()
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun grantedPendingCreationIsRecoveredExactlyOnce() =
        runTest {
            val repository = repository()
            val slotRepository =
                FakeBandalartSlotRepository(
                    maxSlots = 4,
                    initialPendingRewardedCreation =
                        PendingRewardedCreation(
                            requestId = 42L,
                            targetSlots = 4,
                            isGranted = true,
                            templateId = BandalartTemplateId.TRAVEL_PLAN_V1,
                        ),
                )

            presenter(repository, slotRepository).test {
                awaitCreated()

                assertEquals(1, repository.createCalls)
                assertEquals(listOf(BandalartTemplateId.TRAVEL_PLAN_V1), repository.createdTemplateIds)
                assertNull(slotRepository.pendingRewardedCreation)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun legacyGrantedPendingCreationWithoutTemplateRecoversAsBlankBandalart() =
        runTest {
            val repository = repository()
            val slotRepository =
                FakeBandalartSlotRepository(
                    maxSlots = 4,
                    initialPendingRewardedCreation =
                        PendingRewardedCreation(
                            requestId = 43L,
                            targetSlots = 4,
                            isGranted = true,
                            templateId = null,
                        ),
                )

            presenter(repository, slotRepository).test {
                awaitCreated()

                assertEquals(1, repository.createCalls)
                assertEquals(listOf(null), repository.createdTemplateIds)
                assertNull(slotRepository.pendingRewardedCreation)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun presenter(
        repository: FakeBandalartRepository,
        slotRepository: FakeBandalartSlotRepository,
    ) = HomePresenter(
        navigator = FakeNavigator(HomeScreen),
        bandalartRepository = repository,
        bandalartSlotRepository = slotRepository,
        inAppUpdateRepository = FakeInAppUpdateRepository(),
        settingsRepository = FakeSettingsRepository(),
    )

    private fun repository() =
        FakeBandalartRepository(
            initialBandalarts = List(3) { index -> bandalart(index + 1L) },
            recentBandalartId = 1L,
            createdBandalart = bandalart(4L),
        )

    private suspend fun ReceiveTurbine<HomeScreen.State>.awaitLoaded(): HomeScreen.State {
        var state = awaitItem()
        while (state.bandalartList.size != 3 || state.bandalartData?.id != 1L || state.isLoading) {
            state = awaitItem()
        }
        return state
    }

    private suspend fun ReceiveTurbine<HomeScreen.State>.awaitDialog(): HomeScreen.State {
        var state = awaitItem()
        while (state.dialog != HomeScreen.DialogState.RewardedCreate) {
            state = awaitItem()
        }
        return state
    }

    private suspend fun ReceiveTurbine<HomeScreen.State>.awaitRewardedAdRequest(): Long {
        var state = awaitItem()
        while (state.rewardedAdRequestId == null) {
            state = awaitItem()
        }
        return requireNotNull(state.rewardedAdRequestId)
    }

    private suspend fun ReceiveTurbine<HomeScreen.State>.awaitCreated(): HomeScreen.State {
        var state = awaitItem()
        while (state.bandalartData?.id != 4L || state.effect != HomeScreen.Effect.ShowCreateSnackbar) {
            state = awaitItem()
        }
        return state
    }

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
}
