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

package com.nexters.bandalart.navigation

import androidx.compose.runtime.Composable
import com.nexters.bandalart.core.navigation.LegacyHomeScreen
import com.nexters.bandalart.feature.complete.CompleteScreen
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

@AssistedInject
class LegacyHomePresenter(
    @Assisted private val navigator: Navigator,
) : Presenter<LegacyHomeScreen.State> {
    @Composable
    override fun present(): LegacyHomeScreen.State =
        LegacyHomeScreen.State { event ->
            when (event) {
                is LegacyHomeScreen.Event.NavigateToComplete -> {
                    navigator.goTo(
                        CompleteScreen(
                            bandalartId = event.bandalartId,
                            bandalartTitle = event.bandalartTitle,
                            bandalartProfileEmoji = event.bandalartProfileEmoji,
                            bandalartChartImageUri = event.bandalartChartImageUri,
                        ),
                    )
                }
            }
        }

    @CircuitInject(LegacyHomeScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(
            @Assisted navigator: Navigator
        ): LegacyHomePresenter
    }
}
