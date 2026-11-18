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
import com.nexters.bandalart.core.domain.backup.StartupBackupDecision
import com.nexters.bandalart.core.domain.backup.StartupBackupPolicy
import com.nexters.bandalart.core.domain.repository.OnboardingRepository
import com.nexters.bandalart.core.navigation.CloudBackupScreen
import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.onboarding.OnboardingRecordIntroScreen
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
    private val startupBackupPolicy: StartupBackupPolicy,
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
                            val onboardingCompleted = repository.getOnboardingCompletedStatus()
                            // 온보딩을 이미 끝낸 사용자에게는 새로 추가한 달성 기록 소개 장만 한 번 보여 준다.
                            val home =
                                if (onboardingCompleted && !repository.getOnboardingRecordIntroShown()) {
                                    OnboardingRecordIntroScreen
                                } else {
                                    HomeScreen
                                }
                            val destination =
                                when (val decision = startupBackupPolicy.evaluate()) {
                                    StartupBackupDecision.Continue -> {
                                        if (onboardingCompleted) home else OnboardingScreen
                                    }
                                    is StartupBackupDecision.OfferRestore ->
                                        CloudBackupScreen(
                                            entryPoint = CloudBackupScreen.EntryPoint.STARTUP,
                                            fallback =
                                                if (onboardingCompleted) {
                                                    CloudBackupScreen.Fallback.HOME
                                                } else {
                                                    CloudBackupScreen.Fallback.ONBOARDING
                                                },
                                            backupCount = decision.metadata.bandalartCount,
                                            backupUpdatedAt = decision.metadata.updatedAt,
                                        )
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
