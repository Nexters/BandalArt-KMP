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
import androidx.compose.foundation.layout.widthIn
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
import bandalart.core.designsystem.generated.resources.action_save
import bandalart.core.designsystem.generated.resources.action_share
import bandalart.core.designsystem.generated.resources.complete_title
import com.eygraber.uri.Uri
import com.nexters.bandalart.core.common.ImageHandlerProvider
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily
import com.nexters.bandalart.core.ui.component.BandalartButton
import com.nexters.bandalart.core.ui.component.LottieImage
import com.nexters.bandalart.feature.complete.ui.CompleteBandalart
import com.nexters.bandalart.feature.complete.ui.CompleteTopBar
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import org.jetbrains.compose.resources.stringResource

private const val FINISH_LOTTIE_FILE = "files/finish.json"
private val COMPLETE_CONTENT_MAX_WIDTH = 480.dp

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
                // 태블릿·펼친 폴더블에서 카드와 버튼이 화면 전체 너비로 늘어나지 않게 너비를 제한한다.
                // 카드 위치는 화면 가운데의 축하 애니메이션과 겹치지 않도록 기존처럼 전체 영역 기준으로 둔다.
                Box(
                    modifier =
                        Modifier
                            .widthIn(max = COMPLETE_CONTENT_MAX_WIDTH)
                            .fillMaxSize(),
                ) {
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
                            text = stringResource(Res.string.action_save),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                                    .clip(shape = RoundedCornerShape(50.dp))
                                    .background(MaterialTheme.colorScheme.secondary),
                        )
                        BandalartButton(
                            onClick = {
                                eventSink(
                                    CompleteScreen.Event.ShareBandalart(
                                        Uri.parse(state.bandalartChartImageUri),
                                    ),
                                )
                            },
                            text = stringResource(Res.string.action_share),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = 8.dp,
                                        bottom = 32.dp,
                                        start = 24.dp,
                                        end = 24.dp,
                                    ).clip(shape = RoundedCornerShape(50.dp))
                                    .background(MaterialTheme.colorScheme.secondary),
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
