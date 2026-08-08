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

import android.app.Application
import com.nexters.bandalart.core.common.AndroidSupportMailLauncher
import com.nexters.bandalart.core.common.AppVersionProvider
import com.nexters.bandalart.core.common.BannerAdHost
import com.nexters.bandalart.core.common.ImageHandlerProvider
import com.nexters.bandalart.core.common.RewardedAdGateway
import com.nexters.bandalart.core.database.BandalartDatabaseFactory
import com.nexters.bandalart.core.datastore.BandalartDataStoreFactory

private class AndroidPlatformBindings(
    application: Application,
    override val bannerAdHost: BannerAdHost,
    override val rewardedAdGateway: RewardedAdGateway,
) : PlatformBindings {
    override val databaseFactory = BandalartDatabaseFactory(application)
    override val dataStoreFactory = BandalartDataStoreFactory(application)
    override val appVersionProvider = AppVersionProvider(application)
    override val imageHandlerProvider = ImageHandlerProvider(application)
    override val supportMailLauncher = AndroidSupportMailLauncher(application)
}

fun createAndroidAppGraph(
    application: Application,
    bannerAdHost: BannerAdHost,
    rewardedAdGateway: RewardedAdGateway,
): AppGraph = createAppGraph(AndroidPlatformBindings(application, bannerAdHost, rewardedAdGateway))

suspend fun recordRewardedCreation(
    appGraph: AppGraph,
    requestId: Long,
): Boolean = appGraph.bandalartSlotRepository.grantRewardedCreation(requestId) != null
