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

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.nexters.bandalart.core.database.BandalartDao
import com.nexters.bandalart.core.database.BandalartDatabase
import com.nexters.bandalart.core.database.BandalartDatabaseFactory
import com.nexters.bandalart.core.datastore.BandalartDataStore
import com.nexters.bandalart.core.datastore.BandalartDataStoreFactory
import com.nexters.bandalart.core.datastore.InAppUpdateDataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@BindingContainer
object PlatformDataBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideDatabase(factory: BandalartDatabaseFactory): BandalartDatabase =
        factory
            .create()
            .setDriver(BundledSQLiteDriver())
            .build()

    @Provides
    @SingleIn(AppScope::class)
    fun provideBandalartDao(database: BandalartDatabase): BandalartDao = database.bandalartDao

    @Provides
    @SingleIn(AppScope::class)
    fun provideBandalartDataStore(factory: BandalartDataStoreFactory): BandalartDataStore = BandalartDataStore(factory.createBandalartDataStore())

    @Provides
    @SingleIn(AppScope::class)
    fun provideInAppUpdateDataStore(factory: BandalartDataStoreFactory): InAppUpdateDataStore =
        InAppUpdateDataStore(factory.createInAppUpdateDataStore())
}
