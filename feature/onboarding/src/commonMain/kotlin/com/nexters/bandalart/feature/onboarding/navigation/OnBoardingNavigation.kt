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

package com.nexters.bandalart.feature.onboarding.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nexters.bandalart.core.navigation.Route
import com.nexters.bandalart.feature.onboarding.OnBoardingRoute

fun NavController.navigateToOnBoarding(navOptions: NavOptions? = null) {
    this.navigate(Route.Onboarding, navOptions)
}

fun NavGraphBuilder.onBoardingScreen(navigateToHome: (NavOptions) -> Unit) {
    composable<Route.Onboarding> {
        OnBoardingRoute(navigateToHome = navigateToHome)
    }
}
