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

package com.nexters.bandalart.feature.complete

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.complete_save
import bandalart.core.designsystem.generated.resources.complete_share
import bandalart.core.designsystem.generated.resources.complete_title
import com.eygraber.uri.Uri
import com.nexters.bandalart.core.common.ImageHandlerProvider
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily
import com.nexters.bandalart.core.navigation.CommonParcelize
import com.nexters.bandalart.core.ui.component.BandalartButton
import com.nexters.bandalart.core.ui.component.LottieImage
import com.nexters.bandalart.feature.complete.ui.CompleteBandalart
import com.nexters.bandalart.feature.complete.ui.CompleteTopBar
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import org.jetbrains.compose.resources.stringResource

private const val FINISH_LOTTIE_FILE = "files/finish.json"

@CommonParcelize
data class CompleteScreen(
    val bandalartId: Long,
    val bandalartTitle: String,
    val bandalartProfileEmoji: String,
    val bandalartChartImageUri: String,
) : ParcelableScreen {
    data class State(
        val id: Long,
        val title: String,
        val profileEmoji: String,
        val bandalartChartImageUri: String,
        val sideEffect: SideEffect?,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed interface SideEffect {
        data class SaveImage(
            val imageUri: Uri
        ) : SideEffect

        data class ShareImage(
            val imageUri: Uri
        ) : SideEffect
    }

    sealed interface Event : CircuitUiEvent {
        data object NavigateBack : Event

        data class SaveBandalart(
            val imageUri: Uri
        ) : Event

        data class ShareBandalart(
            val imageUri: Uri
        ) : Event

        data object ClearSideEffect : Event
    }
}

@CircuitInject(CompleteScreen::class, AppScope::class)
@Inject
@Composable
internal fun Complete(
    state: CompleteScreen.State,
    modifier: Modifier,
    imageHandlerProvider: ImageHandlerProvider,
) {
    HandleCompleteEffects(
        state = state,
        imageHandlerProvider = imageHandlerProvider,
    )

    CompleteContent(
        state = state,
        modifier = modifier,
    )
}

@Composable
internal fun CompleteContent(
    state: CompleteScreen.State,
    modifier: Modifier = Modifier,
) {
    val eventSink = state.eventSink

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box {
            LottieImage(
                jsonString = FINISH_LOTTIE_FILE,
                iterations = Int.MAX_VALUE,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .align(Alignment.TopCenter),
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                CompleteTopBar(
                    onNavigateBack = {
                        eventSink(CompleteScreen.Event.NavigateBack)
                    },
                )
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = stringResource(Res.string.complete_title),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = pretendardFontFamily(),
                    fontWeight = FontWeight.W700,
                    fontSize = 22.sp,
                    lineHeight = 30.8.sp,
                    textAlign = TextAlign.Center,
                )
                Box(modifier = Modifier.fillMaxSize()) {
                    CompleteBandalart(
                        profileEmoji = state.profileEmoji,
                        title = state.title,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    Column(
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
                        BandalartButton(
                            onClick = {
                                eventSink(
                                    CompleteScreen.Event.SaveBandalart(
                                        Uri.parse(state.bandalartChartImageUri),
                                    ),
                                )
                            },
                            text = stringResource(Res.string.complete_save),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                                    .clip(shape = RoundedCornerShape(50.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                        )
                        BandalartButton(
                            onClick = {
                                eventSink(
                                    CompleteScreen.Event.ShareBandalart(
                                        Uri.parse(state.bandalartChartImageUri),
                                    ),
                                )
                            },
                            text = stringResource(Res.string.complete_share),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = 8.dp,
                                        bottom = 32.dp,
                                        start = 24.dp,
                                        end = 24.dp,
                                    ).clip(shape = RoundedCornerShape(50.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun CompleteScreenPreview() {
    BandalartTheme {
        CompleteContent(
            state =
                CompleteScreen.State(
                    id = 0L,
                    title = "발전하는 예진",
                    profileEmoji = "😎",
                    bandalartChartImageUri = "",
                    sideEffect = null,
                    eventSink = {},
                ),
        )
    }
}
