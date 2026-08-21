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

package com.nexters.bandalart.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.create_bandalart
import bandalart.core.designsystem.generated.resources.delete_bandalart
import bandalart.core.designsystem.generated.resources.rewarded_ad_unavailable
import bandalart.core.designsystem.generated.resources.rewarded_slot_error
import bandalart.core.designsystem.generated.resources.please_input_main_goal
import bandalart.core.designsystem.generated.resources.save_bandalart_image
import bandalart.core.designsystem.generated.resources.settings_contact_body
import bandalart.core.designsystem.generated.resources.settings_contact_fallback
import bandalart.core.designsystem.generated.resources.settings_contact_subject
import bandalart.core.designsystem.generated.resources.settings_deadline_reminder_test_sent
import bandalart.core.designsystem.generated.resources.settings_deadline_reminder_test_failed
import com.nexters.bandalart.core.common.AppVersionProvider
import com.nexters.bandalart.core.common.BannerAdHost
import com.nexters.bandalart.core.common.ImageHandlerProvider
import com.nexters.bandalart.core.common.RewardedAdGateway
import com.nexters.bandalart.core.common.RewardedAdPurpose
import com.nexters.bandalart.core.common.RewardedAdResult
import com.nexters.bandalart.core.common.SupportMailDraft
import com.nexters.bandalart.core.common.SupportMailLauncher
import com.nexters.bandalart.core.common.SupportMailOpenResult
import com.nexters.bandalart.core.common.extension.captureToGraphicsLayer
import com.nexters.bandalart.core.common.openWithClipboardFallback
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.ui.LocalShowSnackbar
import com.nexters.bandalart.feature.home.model.BandalartUiModel
import com.nexters.bandalart.feature.home.model.dummy.dummyBandalartChartData
import com.nexters.bandalart.feature.home.model.dummy.dummyBandalartData
import com.nexters.bandalart.feature.home.model.dummy.dummyBandalartList
import com.nexters.bandalart.feature.home.ui.HomeHeader
import com.nexters.bandalart.feature.home.ui.HomeShareButton
import com.nexters.bandalart.feature.home.ui.HomeTopBar
import com.nexters.bandalart.feature.home.ui.bandalart.BandalartChart
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import io.github.compose.jindong.Jindong
import io.github.compose.jindong.core.model.HapticIntensity
import io.github.compose.jindong.dsl.Haptic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.getString
import kotlin.time.Duration.Companion.milliseconds

private const val SNACKBAR_DURATION_MILLIS = 1500L
internal const val HOME_SCROLL_TAG = "home_scroll"

@CircuitInject(HomeScreen::class, AppScope::class)
@Inject
@Composable
internal fun Home(
    state: HomeScreen.State,
    modifier: Modifier,
    appVersionProvider: AppVersionProvider,
    imageHandlerProvider: ImageHandlerProvider,
    supportMailLauncher: SupportMailLauncher,
    bannerAdHost: BannerAdHost,
    rewardedAdGateway: RewardedAdGateway,
) {
    val homeGraphicsLayer = rememberGraphicsLayer()
    val completeGraphicsLayer = rememberGraphicsLayer()
    val updateSnackbarHostState = remember { SnackbarHostState() }
    val appVersion = remember(appVersionProvider) { appVersionProvider.getAppVersion() }
    LaunchedEffect(state.rewardedAdRequestId) {
        val requestId = state.rewardedAdRequestId ?: return@LaunchedEffect
        val result =
            try {
                rewardedAdGateway.show(requestId, RewardedAdPurpose.BANDALART_CREATION)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                RewardedAdResult.FAILED
            }
        state.eventSink(
            HomeScreen.Event.RewardedAdFinished(
                requestId = requestId,
                result = result,
            ),
        )
        rewardedAdGateway.consume(requestId)
    }

    FlexibleUpdateEffect(
        updateVersionCode = state.updateVersionCode,
        snackbarHostState = updateSnackbarHostState,
        onUpdateAvailable = { versionCode ->
            state.eventSink(HomeScreen.Event.CheckForUpdate(versionCode))
        },
        onUpdateCanceled = {
            state.eventSink(HomeScreen.Event.CancelUpdate)
        },
    )

    DeadlineReminderPermissionEffect(
        requestId = state.deadlinePermissionRequestId,
        onResult = { state.eventSink(HomeScreen.Event.DeadlineReminderPermissionResult) },
    )
    DeadlineReminderForegroundEffect {
        state.eventSink(HomeScreen.Event.DeadlineReminderForegrounded)
    }

    HandleHomeEffects(
        state = state,
        homeGraphicsLayer = homeGraphicsLayer,
        completeGraphicsLayer = completeGraphicsLayer,
        imageHandlerProvider = imageHandlerProvider,
        appVersion = appVersion,
        supportMailLauncher = supportMailLauncher,
    )

    HomeBottomSheets(
        bottomSheet = state.bottomSheet,
        recentEmojis = state.recentEmojis,
        bandalartList = state.bandalartList,
        themeMode = state.themeMode,
        deadlineReminderEnabled = state.deadlineReminderEnabled,
        deadlineNotificationAuthorizationStatus = state.deadlineNotificationAuthorizationStatus,
        deadlineReminderSchedulingHealth = state.deadlineReminderSchedulingHealth,
        eventSink = state.eventSink,
        appVersion = appVersion,
    )
    HomeDialogs(
        dialog = state.dialog,
        bandalartData = state.bandalartData,
        eventSink = state.eventSink,
    )

    HomeContent(
        bandalartListSize = state.bandalartList.size,
        bandalartData = state.bandalartData,
        bandalartCellData = state.bandalartCellData,
        isDropDownMenuOpened = state.isDropDownMenuOpened,
        isBannerCreativeVisible = state.isBannerCreativeVisible(),
        eventSink = state.eventSink,
        homeGraphicsLayer = homeGraphicsLayer,
        completeGraphicsLayer = completeGraphicsLayer,
        updateSnackbarHostState = updateSnackbarHostState,
        bannerAdHost = bannerAdHost,
        modifier = modifier,
    )
}

@Composable
private fun HandleHomeEffects(
    state: HomeScreen.State,
    homeGraphicsLayer: GraphicsLayer,
    completeGraphicsLayer: GraphicsLayer,
    imageHandlerProvider: ImageHandlerProvider,
    appVersion: String,
    supportMailLauncher: SupportMailLauncher,
) {
    val showSnackbar = LocalShowSnackbar.current
    val hapticEffect = state.effect as? HomeScreen.Effect.PlayTaskCompletionHaptic

    if (hapticEffect != null) {
        Jindong(hapticEffect.taskCellId) {
            Haptic(
                duration = TASK_COMPLETION_HAPTIC_MILLIS.milliseconds,
                intensity = HapticIntensity.MEDIUM,
            )
        }
    }

    LaunchedEffect(state.effect) {
        when (state.effect) {
            HomeScreen.Effect.ShowCreateSnackbar -> {
                showSnackbarForDuration(getString(Res.string.create_bandalart), showSnackbar)
            }

            HomeScreen.Effect.ShowDeleteSnackbar -> {
                showSnackbarForDuration(getString(Res.string.delete_bandalart), showSnackbar)
            }

            HomeScreen.Effect.ShowAdUnavailableSnackbar -> {
                showSnackbarForDuration(getString(Res.string.rewarded_ad_unavailable), showSnackbar)
            }

            HomeScreen.Effect.ShowSlotErrorSnackbar -> {
                showSnackbarForDuration(getString(Res.string.rewarded_slot_error), showSnackbar)
            }

            HomeScreen.Effect.ShowDeadlineReminderTestSentSnackbar -> {
                showSnackbarForDuration(getString(Res.string.settings_deadline_reminder_test_sent), showSnackbar)
            }

            HomeScreen.Effect.ShowDeadlineReminderTestFailedSnackbar -> {
                showSnackbarForDuration(getString(Res.string.settings_deadline_reminder_test_failed), showSnackbar)
            }

            HomeScreen.Effect.ShowMainGoalToast -> {
                showToast(getString(Res.string.please_input_main_goal))
            }

            HomeScreen.Effect.OpenSupportMail -> {
                val result =
                    supportMailLauncher.openWithClipboardFallback(
                        SupportMailDraft(
                            subject = getString(Res.string.settings_contact_subject),
                            body =
                                getString(
                                    Res.string.settings_contact_body,
                                    appVersion,
                                    supportMailLauncher.platformName,
                                ),
                        ),
                    )
                if (result != SupportMailOpenResult.OPENED) {
                    showSnackbarForDuration(
                        getString(Res.string.settings_contact_fallback),
                        showSnackbar,
                    )
                }
            }

            is HomeScreen.Effect.PlayTaskCompletionHaptic -> Unit

            null -> Unit
        }

        if (state.effect != null) {
            state.eventSink(HomeScreen.Event.ConsumeEffect)
        }
    }

    LaunchedEffect(state.imageRequest) {
        val request = state.imageRequest ?: return@LaunchedEffect
        withFrameNanos { }

        when (request) {
            HomeScreen.ImageRequest.Share -> {
                imageHandlerProvider.externalShareForBitmap(homeGraphicsLayer.toImageBitmap())
                state.eventSink(HomeScreen.Event.ImageRequestHandled)
            }

            HomeScreen.ImageRequest.Save -> {
                imageHandlerProvider.saveImageToGallery(completeGraphicsLayer.toImageBitmap())
                showToast(getString(Res.string.save_bandalart_image))
                state.eventSink(HomeScreen.Event.ImageRequestHandled)
            }

            is HomeScreen.ImageRequest.Complete -> {
                val imageUri = imageHandlerProvider.bitmapToFileUri(completeGraphicsLayer.toImageBitmap())
                if (imageUri != null) {
                    state.eventSink(HomeScreen.Event.CaptureFinished(imageUri.toString()))
                } else {
                    state.eventSink(HomeScreen.Event.ImageRequestHandled)
                }
            }
        }
    }
}

private const val TASK_COMPLETION_HAPTIC_MILLIS = 50

private suspend fun showSnackbarForDuration(
    message: String,
    showSnackbar: suspend (String) -> Boolean,
) {
    coroutineScope {
        val snackbarJob = launch { showSnackbar(message) }
        delay(SNACKBAR_DURATION_MILLIS)
        snackbarJob.cancel()
    }
}

@Composable
internal fun HomeContent(
    bandalartListSize: Int,
    bandalartData: BandalartUiModel?,
    bandalartCellData: BandalartCellEntity?,
    isDropDownMenuOpened: Boolean,
    isBannerCreativeVisible: Boolean,
    eventSink: (HomeScreen.Event) -> Unit,
    homeGraphicsLayer: GraphicsLayer,
    completeGraphicsLayer: GraphicsLayer,
    updateSnackbarHostState: SnackbarHostState,
    bannerAdHost: BannerAdHost,
    modifier: Modifier = Modifier,
) {
    val isContentReady = bandalartCellData != null && bandalartData != null
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .testTag(HOME_SCROLL_TAG)
                            .padding(bottom = 32.dp),
                ) {
                    HomeTopBar(
                        bandalartCount = bandalartListSize,
                        onHomeUiAction = eventSink,
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Column(
                        modifier =
                            Modifier
                                .captureBandalartToGraphicsLayer(homeGraphicsLayer),
                    ) {
                        if (isContentReady) {
                            HomeHeader(
                                bandalartData = bandalartData,
                                cellData = bandalartCellData,
                                isDropDownMenuOpened = isDropDownMenuOpened,
                                onHomeUiAction = eventSink,
                            )
                            BandalartChart(
                                bandalartData = bandalartData,
                                bandalartCellData = bandalartCellData,
                                onHomeUiAction = eventSink,
                                modifier =
                                    Modifier
                                        .captureBandalartToGraphicsLayer(completeGraphicsLayer),
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (isContentReady) {
                        HomeShareButton(
                            onShareButtonClick = {
                                eventSink(HomeScreen.Event.RequestShare)
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }

                SnackbarHost(
                    hostState = updateSnackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            bannerAdHost.Content(
                visible = isBannerCreativeVisible,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun Modifier.captureBandalartToGraphicsLayer(graphicsLayer: GraphicsLayer): Modifier =
    captureToGraphicsLayer(
        graphicsLayer = graphicsLayer,
        captureBackgroundColor = MaterialTheme.colorScheme.background,
    )

internal fun HomeScreen.State.isBannerCreativeVisible(): Boolean =
    bandalartData != null &&
        bandalartCellData != null &&
        bottomSheet == null &&
        dialog == null &&
        imageRequest == null &&
        rewardedAdRequestId == null

@Preview
@Composable
private fun HomeScreenPreview() {
    BandalartTheme {
        HomeContent(
            bandalartListSize = dummyBandalartList.size,
            bandalartData = dummyBandalartData,
            bandalartCellData = dummyBandalartChartData,
            isDropDownMenuOpened = false,
            isBannerCreativeVisible = true,
            eventSink = {},
            homeGraphicsLayer = rememberGraphicsLayer(),
            completeGraphicsLayer = rememberGraphicsLayer(),
            updateSnackbarHostState = remember { SnackbarHostState() },
            bannerAdHost = com.nexters.bandalart.core.common.NoOpBannerAdHost,
        )
    }
}
