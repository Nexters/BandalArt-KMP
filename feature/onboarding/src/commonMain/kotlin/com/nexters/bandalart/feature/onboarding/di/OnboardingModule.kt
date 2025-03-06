package com.nexters.bandalart.feature.onboarding.di

import com.nexters.bandalart.feature.onboarding.OnboardingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingModule = module {
    viewModelOf(::OnboardingViewModel)
}
