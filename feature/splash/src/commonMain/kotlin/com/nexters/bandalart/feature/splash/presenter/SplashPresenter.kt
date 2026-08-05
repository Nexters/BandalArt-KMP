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

package com.nexters.bandalart.feature.splash.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.nexters.bandalart.core.domain.repository.OnboardingRepository
import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.onboarding.OnboardingScreen
import com.nexters.bandalart.feature.splash.SplashScreen
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.launch

@AssistedInject
class SplashPresenter(
    @Assisted private val navigator: Navigator,
    private val repository: OnboardingRepository,
) : Presenter<SplashScreen.State> {
    @Composable
    override fun present(): SplashScreen.State {
        val scope = rememberCoroutineScope()
        var isChecking by remember { mutableStateOf(false) }

        return SplashScreen.State { event ->
            when (event) {
                SplashScreen.Event.CheckOnboardingStatus -> {
                    if (!isChecking) {
                        isChecking = true
                        scope.launch {
                            val destination =
                                if (repository.getOnboardingCompletedStatus()) {
                                    HomeScreen
                                } else {
                                    OnboardingScreen
                                }
                            navigator.resetRoot(destination)
                        }
                    }
                }
            }
        }
    }

    @CircuitInject(SplashScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(
            @Assisted navigator: Navigator
        ): SplashPresenter
    }
}
