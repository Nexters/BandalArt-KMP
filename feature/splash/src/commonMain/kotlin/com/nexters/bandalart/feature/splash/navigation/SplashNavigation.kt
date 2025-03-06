package com.nexters.bandalart.feature.splash.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nexters.bandalart.core.navigation.Route
import com.nexters.bandalart.feature.splash.SplashRoute

fun NavGraphBuilder.splashScreen(
    navigateToOnBoarding: (NavOptions) -> Unit,
    navigateToHome: (NavOptions) -> Unit,
) {
    composable<Route.Splash> {
        SplashRoute(
            navigateToOnBoarding = navigateToOnBoarding,
            navigateToHome = navigateToHome,
        )
    }
}
