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

package com.nexters.bandalart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.di.metro.AppGraph
import com.nexters.bandalart.feature.splash.SplashScreen
import com.nexters.bandalart.navigation.LocalShowSnackbar
import com.nexters.bandalart.ui.BandalartSnackbar
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import org.koin.compose.KoinContext

@Composable
fun BandalartApp(appGraph: AppGraph) {
    key(appGraph) {
        BandalartTheme {
            KoinContext {
                val snackbarHostState = remember { SnackbarHostState() }
                val backStack = rememberSaveableBackStack(root = SplashScreen)
                val navigator = rememberBandalartNavigator(backStack)
                val showSnackbar: suspend (String) -> Boolean = { message ->
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short,
                    ) == SnackbarResult.ActionPerformed
                }

                CircuitCompositionLocals(appGraph.circuit) {
                    CompositionLocalProvider(LocalShowSnackbar provides showSnackbar) {
                        Scaffold(
                            snackbarHost = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.TopCenter,
                                ) {
                                    SnackbarHost(
                                        modifier =
                                            Modifier
                                                .padding(top = 96.dp)
                                                .height(36.dp),
                                        hostState = snackbarHostState,
                                        snackbar = {
                                            BandalartSnackbar(message = it.visuals.message)
                                        },
                                    )
                                }
                            },
                        ) { innerPadding ->
                            NavigableCircuitContent(
                                navigator = navigator,
                                backStack = backStack,
                                modifier = Modifier.padding(innerPadding),
                            )
                        }
                    }
                }
            }
        }
    }
}
