package com.nexters.bandalart.feature.splash.presenter

import com.nexters.bandalart.core.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeOnboardingRepository(isCompleted: Boolean) : OnboardingRepository {
    private val completed = MutableStateFlow(isCompleted)

    override suspend fun setOnboardingCompletedStatus(flag: Boolean) {
        completed.value = flag
    }

    override fun flowIsOnboardingCompleted(): Flow<Boolean> = completed
}
