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

import com.nexters.bandalart.core.common.AppVersionProvider
import com.nexters.bandalart.core.common.ImageHandlerProvider
import com.nexters.bandalart.core.common.RewardedAdGateway
import com.nexters.bandalart.core.common.SupportMailLauncher
import com.nexters.bandalart.core.database.BandalartDao
import com.nexters.bandalart.core.database.BandalartDatabase
import com.nexters.bandalart.core.database.BandalartDatabaseFactory
import com.nexters.bandalart.core.datastore.BandalartDataStore
import com.nexters.bandalart.core.datastore.BandalartDataStoreFactory
import com.nexters.bandalart.core.datastore.InAppUpdateDataStore
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import com.nexters.bandalart.core.domain.repository.BandalartSlotRepository
import com.nexters.bandalart.core.domain.repository.InAppUpdateRepository
import com.nexters.bandalart.core.domain.repository.OnboardingRepository
import com.nexters.bandalart.core.domain.repository.SettingsRepository
import com.slack.circuit.foundation.Circuit
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.createGraphFactory

interface PlatformBindings {
    val databaseFactory: BandalartDatabaseFactory
    val dataStoreFactory: BandalartDataStoreFactory
    val appVersionProvider: AppVersionProvider
    val imageHandlerProvider: ImageHandlerProvider
    val supportMailLauncher: SupportMailLauncher
    val rewardedAdGateway: RewardedAdGateway
}

@DependencyGraph(
    scope = AppScope::class,
    bindingContainers = [
        PlatformDataBindings::class,
        RepositoryBindings::class,
        CircuitBindings::class,
    ],
)
interface AppGraph {
    val database: BandalartDatabase
    val bandalartDao: BandalartDao
    val bandalartDataStore: BandalartDataStore
    val inAppUpdateDataStore: InAppUpdateDataStore
    val appVersionProvider: AppVersionProvider
    val imageHandlerProvider: ImageHandlerProvider
    val supportMailLauncher: SupportMailLauncher
    val rewardedAdGateway: RewardedAdGateway
    val bandalartRepository: BandalartRepository
    val bandalartSlotRepository: BandalartSlotRepository
    val inAppUpdateRepository: InAppUpdateRepository
    val onboardingRepository: OnboardingRepository
    val settingsRepository: SettingsRepository
    val circuit: Circuit

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Includes platformBindings: PlatformBindings,
        ): AppGraph
    }
}

fun createAppGraph(platformBindings: PlatformBindings): AppGraph = createGraphFactory<AppGraph.Factory>().create(platformBindings)
