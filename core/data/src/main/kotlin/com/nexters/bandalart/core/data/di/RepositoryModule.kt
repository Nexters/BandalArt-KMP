package com.nexters.bandalart.core.data.di

import com.nexters.bandalart.core.data.repository.BandalartRepositoryImpl
import com.nexters.bandalart.core.data.repository.BandalartSlotRepositoryImpl
import com.nexters.bandalart.core.data.repository.GuestLoginTokenRepositoryImpl
import com.nexters.bandalart.core.data.repository.InAppUpdateRepositoryImpl
import com.nexters.bandalart.core.data.repository.OnboardingRepositoryImpl
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import com.nexters.bandalart.core.domain.repository.BandalartSlotRepository
import com.nexters.bandalart.core.domain.repository.GuestLoginTokenRepository
import com.nexters.bandalart.core.domain.repository.InAppUpdateRepository
import com.nexters.bandalart.core.domain.repository.OnboardingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGuestLoginTokenRepository(
        guestLoginTokenRepositoryImpl: GuestLoginTokenRepositoryImpl,
    ): GuestLoginTokenRepository

    @Binds
    @Singleton
    abstract fun bindBandalartRepository(
        bandalartRepositoryImpl: BandalartRepositoryImpl,
    ): BandalartRepository

    @Binds
    @Singleton
    abstract fun bindBandalartSlotRepository(
        bandalartSlotRepositoryImpl: BandalartSlotRepositoryImpl,
    ): BandalartSlotRepository

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(
        onboardingRepositoryImpl: OnboardingRepositoryImpl,
    ): OnboardingRepository

    @Binds
    @Singleton
    abstract fun bindInAppUpdateRepository(
        inAppUpdateRepositoryImpl: InAppUpdateRepositoryImpl,
    ): InAppUpdateRepository
}
