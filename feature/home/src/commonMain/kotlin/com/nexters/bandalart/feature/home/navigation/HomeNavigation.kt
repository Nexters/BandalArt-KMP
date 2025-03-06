package com.nexters.bandalart.feature.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nexters.bandalart.core.navigation.Route
import com.nexters.bandalart.feature.home.HomeRoute

fun NavController.navigateToHome(navOptions: NavOptions? = null) {
    this.navigate(Route.Home, navOptions)
}

fun NavGraphBuilder.homeScreen(
    navigateToComplete: (Long, String, String, String) -> Unit,
    onShowSnackbar: suspend (String) -> Boolean,
) {
    composable<Route.Home> {
        HomeRoute(
            navigateToComplete = navigateToComplete,
            onShowSnackbar = onShowSnackbar,
        )
    }
}
