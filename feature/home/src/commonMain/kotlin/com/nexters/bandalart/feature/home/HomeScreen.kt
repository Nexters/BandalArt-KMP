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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.create_bandalart
import bandalart.core.designsystem.generated.resources.delete_bandalart
import bandalart.core.designsystem.generated.resources.limit_create_bandalart
import bandalart.core.designsystem.generated.resources.please_input_main_goal
import bandalart.core.designsystem.generated.resources.save_bandalart_image
import com.nexters.bandalart.core.common.AppVersionProvider
import com.nexters.bandalart.core.common.ImageHandlerProvider
import com.nexters.bandalart.core.common.extension.captureToGraphicsLayer
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.designsystem.theme.Gray50
import com.nexters.bandalart.core.ui.LocalShowSnackbar
import com.nexters.bandalart.feature.home.model.dummy.dummyBandalartChartData
import com.nexters.bandalart.feature.home.model.dummy.dummyBandalartData
import com.nexters.bandalart.feature.home.model.dummy.dummyBandalartList
import com.nexters.bandalart.feature.home.ui.HomeHeader
import com.nexters.bandalart.feature.home.ui.HomeShareButton
import com.nexters.bandalart.feature.home.ui.HomeTopBar
import com.nexters.bandalart.feature.home.ui.bandalart.BandalartChart
import com.nexters.bandalart.feature.home.ui.bandalart.BandalartSkeleton
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.getString

private const val SNACKBAR_DURATION_MILLIS = 1500L

@CircuitInject(HomeScreen::class, AppScope::class)
@Inject
@Composable
internal fun Home(
    state: HomeScreen.State,
    modifier: Modifier,
    appVersionProvider: AppVersionProvider,
    imageHandlerProvider: ImageHandlerProvider,
) {
    val homeGraphicsLayer = rememberGraphicsLayer()
    val completeGraphicsLayer = rememberGraphicsLayer()
    val updateSnackbarHostState = remember { SnackbarHostState() }
    val appVersion = remember(appVersionProvider) { appVersionProvider.getAppVersion() }

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

    HandleHomeEffects(
        state = state,
        homeGraphicsLayer = homeGraphicsLayer,
        completeGraphicsLayer = completeGraphicsLayer,
        imageHandlerProvider = imageHandlerProvider,
    )

    HomeBottomSheets(
        state = state,
        eventSink = state.eventSink,
        appVersion = appVersion,
    )
    HomeDialogs(
        state = state,
        eventSink = state.eventSink,
    )

    HomeContent(
        state = state,
        homeGraphicsLayer = homeGraphicsLayer,
        completeGraphicsLayer = completeGraphicsLayer,
        updateSnackbarHostState = updateSnackbarHostState,
        modifier = modifier,
    )
}

@Composable
private fun HandleHomeEffects(
    state: HomeScreen.State,
    homeGraphicsLayer: GraphicsLayer,
    completeGraphicsLayer: GraphicsLayer,
    imageHandlerProvider: ImageHandlerProvider,
) {
    val showSnackbar = LocalShowSnackbar.current

    LaunchedEffect(state.effect) {
        when (state.effect) {
            HomeScreen.Effect.ShowCreateSnackbar -> {
                showSnackbarForDuration(getString(Res.string.create_bandalart), showSnackbar)
            }

            HomeScreen.Effect.ShowDeleteSnackbar -> {
                showSnackbarForDuration(getString(Res.string.delete_bandalart), showSnackbar)
            }

            HomeScreen.Effect.ShowLimitToast -> {
                showToast(getString(Res.string.limit_create_bandalart))
            }

            HomeScreen.Effect.ShowMainGoalToast -> {
                showToast(getString(Res.string.please_input_main_goal))
            }

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
    state: HomeScreen.State,
    homeGraphicsLayer: GraphicsLayer,
    completeGraphicsLayer: GraphicsLayer,
    updateSnackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp),
            ) {
                HomeTopBar(
                    bandalartCount = state.bandalartList.size,
                    onHomeUiAction = state.eventSink,
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Column(
                    modifier =
                        Modifier
                            .captureToGraphicsLayer(homeGraphicsLayer)
                            .background(Gray50),
                ) {
                    if (state.bandalartCellData != null && state.bandalartData != null) {
                        HomeHeader(
                            bandalartData = state.bandalartData,
                            cellData = state.bandalartCellData,
                            isDropDownMenuOpened = state.isDropDownMenuOpened,
                            onHomeUiAction = state.eventSink,
                        )
                        BandalartChart(
                            bandalartData = state.bandalartData,
                            bandalartCellData = state.bandalartCellData,
                            onHomeUiAction = state.eventSink,
                            modifier =
                                Modifier
                                    .captureToGraphicsLayer(completeGraphicsLayer)
                                    .background(Gray50),
                        )
                    }
                    Spacer(modifier = Modifier.height(64.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                HomeShareButton(
                    onShareButtonClick = {
                        state.eventSink(HomeScreen.Event.RequestShare)
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            SnackbarHost(
                hostState = updateSnackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            if (state.isLoading) {
                BandalartSkeleton()
            }
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    BandalartTheme {
        HomeContent(
            state =
                HomeScreen.State(
                    bandalartList = dummyBandalartList.toImmutableList(),
                    bandalartData = dummyBandalartData,
                    bandalartCellData = dummyBandalartChartData,
                    isLoading = false,
                    eventSink = {},
                ),
            homeGraphicsLayer = rememberGraphicsLayer(),
            completeGraphicsLayer = rememberGraphicsLayer(),
            updateSnackbarHostState = remember { SnackbarHostState() },
        )
    }
}
