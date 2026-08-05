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

package com.nexters.bandalart.feature.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.app_icon_description
import bandalart.core.designsystem.generated.resources.ic_app
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.designsystem.theme.Gray50
import com.nexters.bandalart.core.navigation.CommonParcelize
import com.nexters.bandalart.core.ui.component.AppTitle
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@CommonParcelize
data object SplashScreen : ParcelableScreen {
    data class State(
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
        data object CheckOnboardingStatus : Event
    }
}

@CircuitInject(SplashScreen::class, AppScope::class)
@Inject
@Composable
internal fun Splash(
    state: SplashScreen.State,
    modifier: Modifier = Modifier,
) {
    ImmediateUpdateEffect {
        state.eventSink(SplashScreen.Event.CheckOnboardingStatus)
    }

    SplashContent(modifier = modifier)
}

@Composable
internal fun SplashContent(modifier: Modifier = Modifier,) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Gray50,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    imageVector = vectorResource(Res.drawable.ic_app),
                    contentDescription = stringResource(Res.string.app_icon_description),
                )
                Spacer(modifier = Modifier.width(10.dp))
                AppTitle()
            }
        }
    }
}

@Preview
@Composable
private fun SplashScreenPreview() {
    BandalartTheme {
        SplashContent()
    }
}
