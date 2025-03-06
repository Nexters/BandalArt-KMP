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
