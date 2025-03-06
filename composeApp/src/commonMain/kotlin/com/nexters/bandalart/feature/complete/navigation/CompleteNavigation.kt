package com.nexters.bandalart.feature.complete.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nexters.bandalart.feature.complete.CompleteRoute
import com.nexters.bandalart.core.navigation.Route

fun NavController.navigateToComplete(
    navOptions: NavOptions? = null,
    bandalartId: Long,
    bandalartTitle: String,
    bandalartProfileEmoji: String?,
    bandalartChartImageUri: String,
) {
    this.navigate(Route.Complete(bandalartId, bandalartTitle, bandalartProfileEmoji, bandalartChartImageUri), navOptions)
}

fun NavGraphBuilder.completeScreen(
    onNavigateBack: () -> Unit,
) {
    composable<Route.Complete> {
        CompleteRoute(onNavigateBack = onNavigateBack)
    }
}
