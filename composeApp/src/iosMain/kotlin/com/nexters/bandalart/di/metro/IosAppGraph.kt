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
import com.nexters.bandalart.core.common.IosSupportMailLauncher
import com.nexters.bandalart.core.database.BandalartDatabaseFactory
import com.nexters.bandalart.core.datastore.BandalartDataStoreFactory

private object IosPlatformBindings : PlatformBindings {
    override val databaseFactory = BandalartDatabaseFactory()
    override val dataStoreFactory = BandalartDataStoreFactory()
    override val appVersionProvider = AppVersionProvider()
    override val imageHandlerProvider = ImageHandlerProvider()
    override val supportMailLauncher = IosSupportMailLauncher()
}

internal fun createIosAppGraph(): AppGraph = createAppGraph(IosPlatformBindings)
