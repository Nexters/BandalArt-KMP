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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nexters.bandalart.core.navigation.Route
import com.nexters.bandalart.core.navigation.LegacyHomeScreen
import com.nexters.bandalart.feature.complete.navigation.completeScreen
import com.nexters.bandalart.feature.complete.navigation.navigateToComplete
import com.nexters.bandalart.feature.home.navigation.homeScreen
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject

internal val LocalShowSnackbar =
    staticCompositionLocalOf<suspend (String) -> Boolean> {
        error("LocalShowSnackbar is not provided")
    }

@CircuitInject(LegacyHomeScreen::class, AppScope::class)
@Inject
@Composable
internal fun LegacyHome(modifier: Modifier = Modifier,) {
    val onShowSnackbar = LocalShowSnackbar.current
    val navController = rememberNavController()

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Route.Home,
    ) {
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
