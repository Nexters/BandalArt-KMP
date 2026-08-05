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

package com.nexters.bandalart.feature.complete.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import com.nexters.bandalart.feature.complete.CompleteScreen
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

@AssistedInject
class CompletePresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: CompleteScreen,
    private val bandalartRepository: BandalartRepository,
) : Presenter<CompleteScreen.State> {
    @Composable
    override fun present(): CompleteScreen.State {
        var sideEffect by remember { mutableStateOf<CompleteScreen.SideEffect?>(null) }

        LaunchedEffect(screen.bandalartId) {
            bandalartRepository.upsertBandalartId(
                bandalartId = screen.bandalartId,
                isCompleted = true,
            )
        }

        return CompleteScreen.State(
            id = screen.bandalartId,
            title = screen.bandalartTitle,
            profileEmoji = screen.bandalartProfileEmoji,
            bandalartChartImageUri = screen.bandalartChartImageUri,
            sideEffect = sideEffect,
        ) { event ->
            when (event) {
                CompleteScreen.Event.NavigateBack -> navigator.pop()
                is CompleteScreen.Event.SaveBandalart -> {
                    sideEffect = CompleteScreen.SideEffect.SaveImage(event.imageUri)
                }

                is CompleteScreen.Event.ShareBandalart -> {
                    sideEffect = CompleteScreen.SideEffect.ShareImage(event.imageUri)
                }

                CompleteScreen.Event.ClearSideEffect -> sideEffect = null
            }
        }
    }

    @CircuitInject(CompleteScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(
            @Assisted navigator: Navigator,
            @Assisted screen: CompleteScreen,
        ): CompletePresenter
    }
}
