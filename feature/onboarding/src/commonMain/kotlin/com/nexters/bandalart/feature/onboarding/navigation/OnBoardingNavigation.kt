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
