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

package com.nexters.bandalart.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.onboarding_record_intro_confirm
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.ui.component.BandalartButton
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import org.jetbrains.compose.resources.stringResource

private val ONBOARDING_RECORD_INTRO_MAX_WIDTH = 480.dp

@CircuitInject(OnboardingRecordIntroScreen::class, AppScope::class)
@Inject
@Composable
internal fun OnboardingRecordIntro(
    state: OnboardingRecordIntroScreen.State,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        // 온보딩의 페이지 표시 높이만큼 띄워 새 장과 같은 위치에 내용을 둔다.
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            OnboardingRecordPage(
                bottomContent = {
                    BandalartButton(
                        onClick = { state.eventSink(OnboardingRecordIntroScreen.Event.Confirm) },
                        text = stringResource(Res.string.onboarding_record_intro_confirm),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                                .clip(shape = RoundedCornerShape(50.dp))
                                .background(MaterialTheme.colorScheme.secondary),
                    )
                },
                modifier = Modifier.widthIn(max = ONBOARDING_RECORD_INTRO_MAX_WIDTH),
            )
        }
    }
}

@Preview
@Composable
private fun OnboardingRecordIntroPreview() {
    BandalartTheme {
        OnboardingRecordIntro(state = OnboardingRecordIntroScreen.State(eventSink = {}))
    }
}
