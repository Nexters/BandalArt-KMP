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
import androidx.test.core.app.ApplicationProvider
import com.nexters.bandalart.core.common.AppVersionProvider
import com.nexters.bandalart.core.common.ImageHandlerProvider
import com.nexters.bandalart.core.database.BandalartDao
import com.nexters.bandalart.core.database.BandalartDatabase
import com.nexters.bandalart.core.datastore.BandalartDataStore
import com.nexters.bandalart.core.datastore.InAppUpdateDataStore
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.dsl.koinApplication
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

@ExtendWith(RobolectricExtension::class)
@Config(sdk = [35])
@DisplayName("Metro AppGraph")
class AppGraphTest {
    private lateinit var appGraph: AppGraph

    @BeforeEach
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        appGraph = createAndroidAppGraph(application)
    }

    @AfterEach
    fun tearDown() {
        appGraph.database.close()
    }

    @Test
    fun appScopedPlatformDataObjectsAreSingletons() {
        assertSame(appGraph.database, appGraph.database)
        assertSame(appGraph.bandalartDao, appGraph.bandalartDao)
        assertSame(appGraph.bandalartDataStore, appGraph.bandalartDataStore)
        assertSame(appGraph.inAppUpdateDataStore, appGraph.inAppUpdateDataStore)
        assertSame(appGraph.appVersionProvider, appGraph.appVersionProvider)
        assertSame(appGraph.imageHandlerProvider, appGraph.imageHandlerProvider)
    }

    @Test
    fun koinBridgeExposesMetroGraphInstances() {
        val koinApplication =
            koinApplication {
                modules(metroKoinBridgeModule(appGraph))
            }

        with(koinApplication.koin) {
            assertSame(appGraph.database, get<BandalartDatabase>())
            assertSame(appGraph.bandalartDao, get<BandalartDao>())
            assertSame(appGraph.bandalartDataStore, get<BandalartDataStore>())
            assertSame(appGraph.inAppUpdateDataStore, get<InAppUpdateDataStore>())
            assertSame(appGraph.appVersionProvider, get<AppVersionProvider>())
            assertSame(appGraph.imageHandlerProvider, get<ImageHandlerProvider>())
        }

        koinApplication.close()
    }
}
