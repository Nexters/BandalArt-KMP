package com.nexters.bandalart.feature.onboarding.presenter

import com.nexters.bandalart.core.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class FakeOnboardingRepository : OnboardingRepository {
    var isCompleted = false
        private set

    override suspend fun setOnboardingCompletedStatus(flag: Boolean) {
        isCompleted = flag
    }

    override fun flowIsOnboardingCompleted(): Flow<Boolean> = flowOf(isCompleted)
}
