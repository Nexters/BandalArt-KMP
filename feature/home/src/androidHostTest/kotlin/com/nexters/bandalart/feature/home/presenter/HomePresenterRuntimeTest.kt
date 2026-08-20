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
import com.nexters.bandalart.core.domain.entity.ThemeMode
import com.nexters.bandalart.feature.complete.CompleteScreen
import com.nexters.bandalart.feature.home.HomeScreen
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomePresenterRuntimeTest {
    @Test
    fun shareAndSaveRequestsAreClearedAfterUiHandling() =
        runTest {
            val presenter = presenter()

            presenter.test {
                var state = awaitLoadedBandalart()

                state.eventSink(HomeScreen.Event.RequestShare)
                state = awaitItem()
                assertEquals(HomeScreen.ImageRequest.Share, state.imageRequest)

                state.eventSink(HomeScreen.Event.ImageRequestHandled)
                state = awaitItem()
                assertNull(state.imageRequest)

                state.eventSink(HomeScreen.Event.OpenDropDownMenu)
                do {
                    state = awaitItem()
                } while (!state.isDropDownMenuOpened)
                state.eventSink(HomeScreen.Event.RequestSave)
                do {
                    state = awaitItem()
                } while (state.imageRequest != HomeScreen.ImageRequest.Save)
                assertEquals(HomeScreen.ImageRequest.Save, state.imageRequest)
                assertEquals(false, state.isDropDownMenuOpened)

                state.eventSink(HomeScreen.Event.ImageRequestHandled)
                state = awaitItem()
                assertNull(state.imageRequest)
            }
        }

    @Test
    fun settingsSheetReflectsAndPersistsThemeSelection() =
        runTest {
            val settingsRepository = FakeSettingsRepository(ThemeMode.LIGHT)
            val presenter = presenter(settingsRepository = settingsRepository)

            presenter.test {
                var state = awaitLoadedBandalart()
                assertEquals(ThemeMode.LIGHT, state.themeMode)

                state.eventSink(HomeScreen.Event.OpenSettings)
                do {
                    state = awaitItem()
                } while (state.bottomSheet != HomeScreen.BottomSheetState.Settings)

                state.eventSink(HomeScreen.Event.SelectThemeMode(ThemeMode.DARK))
                do {
                    state = awaitItem()
                } while (state.themeMode != ThemeMode.DARK)

                assertEquals(listOf(ThemeMode.DARK), settingsRepository.savedThemeModes)
                state.eventSink(HomeScreen.Event.DismissBottomSheet)
                do {
                    state = awaitItem()
                } while (state.bottomSheet != null)
                assertEquals(ThemeMode.DARK, state.themeMode)
            }
        }

    @Test
    fun contactSupportIsExposedAsOneShotEffect() =
        runTest {
            val presenter = presenter()

            presenter.test {
                var state = awaitLoadedBandalart()

                state.eventSink(HomeScreen.Event.ContactSupport)
                do {
                    state = awaitItem()
                } while (state.effect != HomeScreen.Effect.OpenSupportMail)

                state.eventSink(HomeScreen.Event.ContactSupport)
                expectNoEvents()

                state.eventSink(HomeScreen.Event.ConsumeEffect)
                state = awaitItem()
                assertNull(state.effect)
            }
        }

    @Test
    fun completionNavigatesOnlyAfterCaptureFinishes() =
        runTest {
            val navigator = FakeNavigator(HomeScreen)
            val repository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart(isCompleted = true)),
                    recentBandalartId = 1L,
                    previousBandalartList = listOf(1L to false),
                )
            val presenter =
                HomePresenter(
                    navigator = navigator,
                    bandalartRepository = repository,
                    bandalartSlotRepository = FakeBandalartSlotRepository(),
                    inAppUpdateRepository = FakeInAppUpdateRepository(),
                    settingsRepository = FakeSettingsRepository(),
                )

            presenter.test {
                var state = awaitItem()
                while (state.imageRequest !is HomeScreen.ImageRequest.Complete) {
                    state = awaitItem()
                }

                val request =
                    assertInstanceOf(
                        HomeScreen.ImageRequest.Complete::class.java,
                        state.imageRequest,
                    )
                assertEquals(1L, request.bandalartId)

                state.eventSink(HomeScreen.Event.CaptureFinished("content://bandalart/chart"))

                val destination = assertInstanceOf(CompleteScreen::class.java, navigator.awaitNextScreen())
                assertEquals(1L, destination.bandalartId)
                assertEquals("목표", destination.bandalartTitle)
                assertEquals("content://bandalart/chart", destination.bandalartChartImageUri)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun rejectedFlexibleUpdateIsSkippedButNewerVersionCanBeCanceled() =
        runTest {
            val updateRepository = FakeInAppUpdateRepository(lastRejectedVersionCode = 20206)
            val presenter = presenter(updateRepository)

            presenter.test {
                var state = awaitLoadedBandalart()
                advanceUntilIdle()
                state.eventSink(HomeScreen.Event.CheckForUpdate(20206))
                expectNoEvents()

                state.eventSink(HomeScreen.Event.CheckForUpdate(20207))
                do {
                    state = awaitItem()
                } while (state.updateVersionCode != 20207)

                state.eventSink(HomeScreen.Event.CancelUpdate)
                do {
                    state = awaitItem()
                } while (state.updateVersionCode != null)

                assertEquals(listOf(20207), updateRepository.rejectedVersionCodes)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun presenter(
        updateRepository: FakeInAppUpdateRepository = FakeInAppUpdateRepository(),
        settingsRepository: FakeSettingsRepository = FakeSettingsRepository(),
    ): HomePresenter =
        HomePresenter(
            navigator = FakeNavigator(HomeScreen),
            bandalartRepository =
                FakeBandalartRepository(
                    initialBandalarts = listOf(bandalart()),
                    recentBandalartId = 1L,
                ),
            bandalartSlotRepository = FakeBandalartSlotRepository(),
            inAppUpdateRepository = updateRepository,
            settingsRepository = settingsRepository,
        )

    private suspend fun app.cash.turbine.ReceiveTurbine<HomeScreen.State>.awaitLoadedBandalart(): HomeScreen.State {
        var state = awaitItem()
        while (state.bandalartData == null) {
            state = awaitItem()
        }
        return state
    }

    private fun bandalart(isCompleted: Boolean = false) =
        BandalartEntity(
            id = 1L,
            title = "목표",
            description = "설명",
            profileEmoji = "🎯",
            mainColor = "#3FFFBA",
            subColor = "#111827",
            dueDate = null,
            isCompleted = isCompleted,
            completionRatio = if (isCompleted) 100 else 0,
        )
}
