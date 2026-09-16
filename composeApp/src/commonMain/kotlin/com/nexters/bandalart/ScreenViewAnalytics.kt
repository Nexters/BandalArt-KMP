/*
 * Copyright 2026 easyhooon
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

package com.nexters.bandalart

import androidx.compose.runtime.Composable
import com.nexters.bandalart.core.navigation.CloudBackupScreen
import com.nexters.bandalart.feature.complete.CompleteScreen
import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.onboarding.OnboardingScreen
import com.nexters.bandalart.feature.splash.SplashScreen
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.effects.ImpressionEffect
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.FirebaseAnalyticsEvents
import dev.gitlive.firebase.analytics.FirebaseAnalyticsParam
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.analytics.logEvent

internal data class AnalyticsScreen(
    val name: String,
    val className: String,
)

internal fun Screen.analyticsScreen(): AnalyticsScreen? =
    when (this) {
        SplashScreen -> AnalyticsScreen(name = "splash", className = "SplashScreen")
        OnboardingScreen -> AnalyticsScreen(name = "onboarding", className = "OnboardingScreen")
        HomeScreen -> AnalyticsScreen(name = "home", className = "HomeScreen")
        is CompleteScreen -> AnalyticsScreen(name = "complete", className = "CompleteScreen")
        is CloudBackupScreen -> AnalyticsScreen(name = "cloud_backup", className = "CloudBackupScreen")
        else -> null
    }

@Composable
internal fun TrackScreenView(screen: Screen) {
    val analyticsScreen = screen.analyticsScreen() ?: return
    ImpressionEffect(analyticsScreen.name) {
        Firebase.analytics.logEvent(FirebaseAnalyticsEvents.SCREEN_VIEW) {
            param(FirebaseAnalyticsParam.SCREEN_NAME, analyticsScreen.name)
            param(FirebaseAnalyticsParam.SCREEN_CLASS, analyticsScreen.className)
        }
    }
}
