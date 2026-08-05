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

package com.nexters.bandalart.feature.onboarding.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.nexters.bandalart.core.domain.repository.OnboardingRepository
import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.onboarding.OnboardingScreen
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.launch

@AssistedInject
class OnboardingPresenter(
    @Assisted private val navigator: Navigator,
    private val repository: OnboardingRepository,
) : Presenter<OnboardingScreen.State> {
    @Composable
    override fun present(): OnboardingScreen.State {
        val scope = rememberCoroutineScope()
        var isCompleting by remember { mutableStateOf(false) }

        return OnboardingScreen.State { event ->
            when (event) {
                OnboardingScreen.Event.NavigateToHome -> {
                    if (!isCompleting) {
                        isCompleting = true
                        scope.launch {
                            repository.setOnboardingCompletedStatus(true)
                            navigator.resetRoot(HomeScreen)
                        }
                    }
                }
            }
        }
    }

    @CircuitInject(OnboardingScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(
            @Assisted navigator: Navigator
        ): OnboardingPresenter
    }
}
