/*
 * Copyright 2025 easyhooon
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

package com.nexters.bandalart.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexters.bandalart.core.domain.repository.OnboardingRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface OnBoardingUiEvent {
    data object NavigateToHome : OnBoardingUiEvent
}

class OnboardingViewModel(
    private val onboardingRepository: OnboardingRepository,
) : ViewModel() {
    private val _uiEvent = Channel<OnBoardingUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun setOnboardingCompletedStatus(flag: Boolean) {
        viewModelScope.launch {
            onboardingRepository.setOnboardingCompletedStatus(flag)
            navigateToHome()
        }
    }

    private fun navigateToHome() {
        viewModelScope.launch {
            _uiEvent.send(OnBoardingUiEvent.NavigateToHome)
        }
    }
}
