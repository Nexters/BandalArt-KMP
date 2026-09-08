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

package com.nexters.bandalart.ads

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.nexters.bandalart.R
import com.nexters.bandalart.ads.nativead.NativeAdBodyView
import com.nexters.bandalart.ads.nativead.NativeAdCallToActionView
import com.nexters.bandalart.ads.nativead.NativeAdChoicesView
import com.nexters.bandalart.ads.nativead.NativeAdHeadlineView
import com.nexters.bandalart.ads.nativead.NativeAdMediaView
import com.nexters.bandalart.ads.nativead.NativeAdViewContainer
import com.nexters.bandalart.ads.nativead.rememberNativeAdPreloader
import com.nexters.bandalart.core.common.ExitDialogHost
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily
import com.nexters.bandalart.feature.home.ui.bandalart.BandalartActionAlertDialog

class AndroidExitDialogHost(
    private val awaitAdsInitialized: suspend () -> Boolean,
) : ExitDialogHost {
    @Composable
    override fun Content(enabled: Boolean) {
        val activity = LocalActivity.current ?: return
        var showDialog by remember { mutableStateOf(false) }
        val adSession = remember { ExitDialogAdSession<NativeAd>() }

        val adPreloader =
            rememberNativeAdPreloader(
                adUnitId = activity.getString(R.string.admob_exit_dialog_native_ad_unit_id),
                awaitAdsInitialized = awaitAdsInitialized,
                preload = enabled,
            )

        LaunchedEffect(enabled, adPreloader) {
            if (!enabled) {
                val wasAdDisplayed = adSession.closeAndWasAdDisplayed()
                showDialog = false
                adPreloader.disableLoading()
                if (wasAdDisplayed) adPreloader.discard()
            }
        }

        BackHandler(enabled = enabled) {
            adPreloader.loadIfNeeded()
            adSession.open(adPreloader.adSnapshotForDisplay())
            showDialog = true
        }

        if (enabled && showDialog) {
            BandalartActionAlertDialog(
                icon = null,
                iconContentDescription = null,
                title = activity.getString(R.string.exit_dialog_title),
                message = null,
                confirmLabel = activity.getString(R.string.exit_dialog_confirm),
                cancelLabel = activity.getString(R.string.exit_dialog_cancel),
                onConfirmClick = activity::finish,
                onCancelClick = {
                    val wasAdDisplayed = adSession.closeAndWasAdDisplayed()
                    showDialog = false
                    if (wasAdDisplayed) adPreloader.recycleIfEligible()
                },
                content =
                    adSession.ad?.let { nativeAd ->
                        {
                            NativeAdViewContainer(
                                nativeAd = nativeAd,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                ExitDialogNativeAdContent(
                                    headline = nativeAd.headline.orEmpty(),
                                    body = nativeAd.body,
                                    callToAction = nativeAd.callToAction,
                                    mediaContent = nativeAd.mediaContent,
                                )
                            }
                        }
                    },
            )
        }
    }
}

@Composable
private fun ExitDialogNativeAdContent(
    headline: String,
    body: String?,
    callToAction: String?,
    mediaContent: com.google.android.libraries.ads.mobile.sdk.nativead.MediaContent?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        NativeAdChoicesView(
            modifier =
                Modifier
                    .align(Alignment.End)
                    .size(16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        NativeAdMediaView(
            mediaContent = mediaContent,
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(192.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text =
                    androidx.compose.ui.res
                        .stringResource(R.string.exit_dialog_ad_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = pretendardFontFamily(),
                fontWeight = FontWeight.W600,
                modifier =
                    Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp),
                        ).padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            NativeAdHeadlineView(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontFamily = pretendardFontFamily(),
                    fontWeight = FontWeight.W700,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!body.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            NativeAdBodyView(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = pretendardFontFamily(),
                )
            }
        }
        if (!callToAction.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            NativeAdCallToActionView(
                rippleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                cornerRadius = 12.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = callToAction,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontFamily = pretendardFontFamily(),
                        fontWeight = FontWeight.W600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
