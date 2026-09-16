/*
 * Copyright 2026 easyhooon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nexters.bandalart

import com.nexters.bandalart.core.navigation.CloudBackupScreen
import com.nexters.bandalart.feature.complete.CompleteScreen
import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.onboarding.OnboardingScreen
import com.nexters.bandalart.feature.splash.SplashScreen
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ScreenViewAnalyticsTest {
    @Test
    fun mapsCircuitScreensToStableAnalyticsNames() {
        val screens =
            listOf(
                SplashScreen to "splash",
                OnboardingScreen to "onboarding",
                HomeScreen to "home",
                CompleteScreen(1L, "목표", "🎯", "uri") to "complete",
                CloudBackupScreen(CloudBackupScreen.EntryPoint.SETTINGS) to "cloud_backup",
            )

        screens.forEach { (screen, expectedName) ->
            assertEquals(expectedName, screen.analyticsScreen()?.name)
        }
    }
}
