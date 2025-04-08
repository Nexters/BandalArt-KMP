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

package com.nexters.bandalart.core.data.di

import com.nexters.bandalart.core.domain.repository.BandalartRepository
import com.nexters.bandalart.core.domain.repository.InAppUpdateRepository
import com.nexters.bandalart.core.domain.repository.OnboardingRepository
import com.nexters.bandalart.core.data.repository.DefaultBandalartRepository
import com.nexters.bandalart.core.data.repository.DefaultInAppUpdateRepository
import com.nexters.bandalart.core.data.repository.DefaultOnboardingRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule =
    module {
        singleOf(::DefaultBandalartRepository).bind<BandalartRepository>()
        singleOf(::DefaultInAppUpdateRepository).bind<InAppUpdateRepository>()
        singleOf(::DefaultOnboardingRepository).bind<OnboardingRepository>()
    }
