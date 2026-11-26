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

package com.nexters.bandalart.feature.home.ui.bandalart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.ad_tracking_guide_confirm
import bandalart.core.designsystem.generated.resources.ad_tracking_guide_message
import bandalart.core.designsystem.generated.resources.ad_tracking_guide_title
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily
import com.nexters.bandalart.core.ui.component.BandalartDialogSurface
import com.nexters.bandalart.core.ui.component.bandalartDialogContentAlignment
import com.nexters.bandalart.core.ui.component.bandalartDialogTextAlign
import org.jetbrains.compose.resources.stringResource
import zone.ien.hig.adaptive.AdaptiveButton
import zone.ien.hig.adaptive.ExperimentalAdaptiveApi

/**
 * iOS 광고 추적 권한(ATT) 시스템 팝업 직전에 이유를 설명한다.
 *
 * 닫기 버튼 없이 계속 버튼 하나만 둔다. 여기서 닫으면 ATT가 미결정으로 남아 다음 광고 동의 확인 때
 * AdMob 설명 메시지가 대신 뜨기 때문이다. 허용 여부는 다음 시스템 팝업에서 사용자가 고른다.
 */
@OptIn(ExperimentalAdaptiveApi::class)
@Composable
internal fun AdTrackingGuideDialog(
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentAlignment = bandalartDialogContentAlignment()
    val textAlign = bandalartDialogTextAlign()

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        BandalartDialogSurface {
            Column(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
            ) {
                Text(
                    text = stringResource(Res.string.ad_tracking_guide_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontFamily = pretendardFontFamily(),
                    fontWeight = FontWeight.W700,
                    modifier =
                        Modifier
                            .align(contentAlignment)
                            .padding(horizontal = 24.dp),
                    textAlign = textAlign,
                    lineHeight = 30.sp,
                    letterSpacing = (-0.4).sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.ad_tracking_guide_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontFamily = pretendardFontFamily(),
                    fontWeight = FontWeight.W500,
                    modifier =
                        Modifier
                            .align(contentAlignment)
                            .padding(horizontal = 24.dp),
                    textAlign = textAlign,
                    lineHeight = 21.sp,
                    letterSpacing = (-0.28).sp,
                )
                Spacer(modifier = Modifier.height(30.dp))
                AdaptiveButton(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(56.dp),
                    onClick = onConfirmClick,
                    adaptation = {
                        material {
                            colors =
                                ButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContainerColor = MaterialTheme.colorScheme.primary,
                                    disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                                )
                        }
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.ad_tracking_guide_confirm),
                        fontSize = 16.sp,
                        fontFamily = pretendardFontFamily(),
                        fontWeight = FontWeight.W600,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
