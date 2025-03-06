package com.nexters.bandalart.core.data.di

import com.nexters.bandalart.core.domain.repository.BandalartRepository
import com.nexters.bandalart.core.domain.repository.InAppUpdateRepository
import com.nexters.bandalart.core.domain.repository.OnboardingRepository
import com.nexters.bandalart.core.data.repository.BandalartRepositoryImpl
import com.nexters.bandalart.core.data.repository.InAppUpdateRepositoryImpl
import com.nexters.bandalart.core.data.repository.OnboardingRepositoryImpl
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule = module {
    singleOf(::BandalartRepositoryImpl).bind<BandalartRepository>()
    singleOf(::InAppUpdateRepositoryImpl).bind<InAppUpdateRepository>()
    singleOf(::OnboardingRepositoryImpl).bind<OnboardingRepository>()
}
