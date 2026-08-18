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
import com.nexters.bandalart.core.common.NoOpBannerAdHost
import com.nexters.bandalart.core.common.NoOpRewardedAdGateway
import com.nexters.bandalart.feature.complete.CompleteScreen
import com.nexters.bandalart.core.navigation.CloudBackupScreen
import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.onboarding.OnboardingScreen
import com.nexters.bandalart.feature.splash.SplashScreen
import com.slack.circuit.test.FakeNavigator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
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
        appGraph = createAndroidAppGraph(application, NoOpBannerAdHost, NoOpRewardedAdGateway)
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
        assertSame(NoOpBannerAdHost, appGraph.bannerAdHost)
        assertSame(appGraph.imageHandlerProvider, appGraph.imageHandlerProvider)
        assertSame(appGraph.supportMailLauncher, appGraph.supportMailLauncher)
        assertSame(appGraph.rewardedAdGateway, appGraph.rewardedAdGateway)
        assertSame(appGraph.bandalartRepository, appGraph.bandalartRepository)
        assertSame(appGraph.bandalartWidgetRepository, appGraph.bandalartWidgetRepository)
        assertSame(appGraph.bandalartWidgetLaunchTarget, appGraph.bandalartWidgetLaunchTarget)
        assertSame(appGraph.bandalartSlotRepository, appGraph.bandalartSlotRepository)
        assertSame(appGraph.inAppUpdateRepository, appGraph.inAppUpdateRepository)
        assertSame(appGraph.onboardingRepository, appGraph.onboardingRepository)
        assertSame(appGraph.settingsRepository, appGraph.settingsRepository)
        assertSame(appGraph.cloudBackupRepository, appGraph.cloudBackupRepository)
        assertSame(appGraph.startupBackupPolicy, appGraph.startupBackupPolicy)
        assertSame(appGraph.circuit, appGraph.circuit)
    }

    @Test
    fun cloudBackupIsUnsupportedWithoutApiConfiguration() {
        org.junit.jupiter.api.Assertions
            .assertFalse(appGraph.cloudBackupRepository.isSupported)
    }

    @Test
    fun circuitContainsMigratedScreenFactories() {
        assertNotNull(appGraph.circuit.presenter(SplashScreen, FakeNavigator(SplashScreen)))
        assertNotNull(appGraph.circuit.ui(SplashScreen))
        assertNotNull(appGraph.circuit.presenter(OnboardingScreen, FakeNavigator(OnboardingScreen)))
        assertNotNull(appGraph.circuit.ui(OnboardingScreen))
        val completeScreen =
            CompleteScreen(
                bandalartId = 42L,
                bandalartTitle = "출시 준비",
                bandalartProfileEmoji = "🚀",
                bandalartChartImageUri = "content://bandalart/chart",
            )
        assertNotNull(appGraph.circuit.presenter(completeScreen, FakeNavigator(completeScreen)))
        assertNotNull(appGraph.circuit.ui(completeScreen))
        assertNotNull(appGraph.circuit.presenter(HomeScreen, FakeNavigator(HomeScreen)))
        assertNotNull(appGraph.circuit.ui(HomeScreen))
        val backupScreen = CloudBackupScreen(entryPoint = CloudBackupScreen.EntryPoint.SETTINGS)
        assertNotNull(appGraph.circuit.presenter(backupScreen, FakeNavigator(backupScreen)))
        assertNotNull(appGraph.circuit.ui(backupScreen))
    }
}
