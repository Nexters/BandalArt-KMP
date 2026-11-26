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

import app.cash.turbine.Event
import app.cash.turbine.ReceiveTurbine
import com.nexters.bandalart.core.domain.ads.AdTrackingPermission
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.feature.home.HomeScreen
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomePresenterAdTrackingTest {
    @Test
    fun undeterminedTrackingShowsGuideAndRequestsTrackingBeforeAdConsent() =
        runTest {
            val permission = FakeAdTrackingPermission(isRequestNeeded = true)

            presenter(permission).test {
                var state = awaitItem()
                while (state.dialog == null) state = awaitItem()

                assertEquals(HomeScreen.DialogState.AdTrackingGuide, state.dialog)
                assertEquals(emptyList<String>(), permission.calls)

                state.eventSink(HomeScreen.Event.ConfirmAdTrackingGuide)
                do {
                    state = awaitItem()
                } while (state.dialog != null)
                advanceUntilIdle()

                assertEquals(listOf("request", "startAdConsent"), permission.calls)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun answeredTrackingStartsAdConsentOnceWithoutGuide() =
        runTest {
            val permission = FakeAdTrackingPermission(isRequestNeeded = false)

            presenter(permission).test {
                awaitLoadedBandalart()
                advanceUntilIdle()
                val state =
                    cancelAndConsumeRemainingEvents()
                        .filterIsInstance<Event.Item<HomeScreen.State>>()
                        .lastOrNull()
                        ?.value

                assertNull(state?.dialog)
                assertEquals(listOf("startAdConsent"), permission.calls)
            }
        }

    private fun presenter(permission: AdTrackingPermission) =
        HomePresenter(
            navigator = FakeNavigator(HomeScreen),
            bandalartRepository = repository(),
            bandalartSlotRepository = FakeBandalartSlotRepository(),
            inAppUpdateRepository = FakeInAppUpdateRepository(),
            settingsRepository = FakeSettingsRepository(initialTaskCompletionTooltipDismissed = true),
            adTrackingPermission = permission,
        )

    private fun repository(): FakeBandalartRepository {
        val bandalart =
            BandalartEntity(
                id = 1L,
                mainColor = "#3FFFBA",
                subColor = "#111827",
                profileEmoji = "🎯",
                title = "반다라트",
                description = null,
                dueDate = null,
                isCompleted = false,
                completionRatio = 0,
            )
        val mainCell =
            BandalartCellEntity(
                id = 10L,
                title = "메인",
                description = null,
                dueDate = null,
                isCompleted = false,
                parentId = null,
            )
        return FakeBandalartRepository(
            initialBandalarts = listOf(bandalart),
            recentBandalartId = bandalart.id,
            previousBandalartList = listOf(bandalart.id to false),
            mainCells = mapOf(bandalart.id to mainCell),
            childCells = mapOf(mainCell.id to emptyList()),
        )
    }

    private suspend fun ReceiveTurbine<HomeScreen.State>.awaitLoadedBandalart(): HomeScreen.State {
        var state = awaitItem()
        while (state.bandalartData?.id != 1L) {
            state = awaitItem()
        }
        return state
    }

    private class FakeAdTrackingPermission(
        private val isRequestNeeded: Boolean,
    ) : AdTrackingPermission {
        val calls = mutableListOf<String>()

        override fun isRequestNeeded(): Boolean = isRequestNeeded

        override suspend fun request() {
            calls += "request"
        }

        override fun startAdConsent() {
            calls += "startAdConsent"
        }
    }
}
