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

package com.nexters.bandalart.di.metro

import com.nexters.bandalart.core.data.repository.DefaultBandalartRepository
import com.nexters.bandalart.core.data.repository.DefaultInAppUpdateRepository
import com.nexters.bandalart.core.data.repository.DefaultOnboardingRepository
import com.nexters.bandalart.core.database.BandalartDao
import com.nexters.bandalart.core.datastore.BandalartDataStore
import com.nexters.bandalart.core.datastore.InAppUpdateDataStore
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import com.nexters.bandalart.core.domain.repository.InAppUpdateRepository
import com.nexters.bandalart.core.domain.repository.OnboardingRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@BindingContainer
object RepositoryBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideBandalartRepository(
        bandalartDataStore: BandalartDataStore,
        bandalartDao: BandalartDao,
    ): BandalartRepository =
        DefaultBandalartRepository(bandalartDataStore, bandalartDao)

    @Provides
    @SingleIn(AppScope::class)
    fun provideInAppUpdateRepository(inAppUpdateDataStore: InAppUpdateDataStore): InAppUpdateRepository =
        DefaultInAppUpdateRepository(inAppUpdateDataStore)

    @Provides
    @SingleIn(AppScope::class)
    fun provideOnboardingRepository(bandalartDataStore: BandalartDataStore): OnboardingRepository =
        DefaultOnboardingRepository(bandalartDataStore)
}
