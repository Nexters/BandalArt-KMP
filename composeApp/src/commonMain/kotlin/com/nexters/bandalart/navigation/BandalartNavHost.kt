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

package com.nexters.bandalart.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nexters.bandalart.core.navigation.Route
import com.nexters.bandalart.feature.complete.navigation.completeScreen
import com.nexters.bandalart.feature.complete.navigation.navigateToComplete
import com.nexters.bandalart.feature.home.navigation.homeScreen
import com.nexters.bandalart.feature.home.navigation.navigateToHome
import com.nexters.bandalart.feature.onboarding.navigation.navigateToOnBoarding
import com.nexters.bandalart.feature.onboarding.navigation.onBoardingScreen
import com.nexters.bandalart.feature.splash.navigation.splashScreen

@Composable
fun BandalartNavHost(
    modifier: Modifier = Modifier,
    onShowSnackbar: suspend (String) -> Boolean,
) {
    val navController = rememberNavController()

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Route.Splash,
    ) {
        splashScreen(
            navigateToOnBoarding = navController::navigateToOnBoarding,
            navigateToHome = navController::navigateToHome,
        )
        onBoardingScreen(
            navigateToHome = navController::navigateToHome,
        )
        homeScreen(
            navigateToComplete = { id, title, emoji, imageUri ->
                navController.navigateToComplete(
                    bandalartId = id,
                    bandalartTitle = title,
                    bandalartProfileEmoji = emoji,
                    bandalartChartImageUri = imageUri,
                )
            },
            onShowSnackbar = onShowSnackbar,
        )
        completeScreen(
            onNavigateBack = navController::popBackStack,
        )
    }
}
