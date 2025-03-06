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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bandalart.composeapp.generated.resources.Res
import bandalart.composeapp.generated.resources.complete_save
import bandalart.composeapp.generated.resources.complete_share
import bandalart.composeapp.generated.resources.complete_title
import bandalart.composeapp.generated.resources.save_bandalart_image
import com.nexters.bandalart.core.common.ImageHandlerProvider
import com.nexters.bandalart.core.common.utils.ObserveAsEvents
import com.nexters.bandalart.core.designsystem.theme.Gray50
import com.nexters.bandalart.core.designsystem.theme.Gray900
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily
import com.nexters.bandalart.core.ui.component.BandalartButton
import com.nexters.bandalart.core.ui.component.LottieImage
import com.nexters.bandalart.feature.complete.ui.CompleteBandalart
import com.nexters.bandalart.feature.complete.ui.CompleteTopBar
import com.nexters.bandalart.feature.complete.viewmodel.CompleteUiAction
import com.nexters.bandalart.feature.complete.viewmodel.CompleteUiEvent
import com.nexters.bandalart.feature.complete.viewmodel.CompleteUiState
import com.nexters.bandalart.feature.complete.viewmodel.CompleteViewModel
import kotlinx.coroutines.launch
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private const val FINISH_LOTTIE_FILE = "files/finish.json"

@Composable
internal fun CompleteRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CompleteViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val imageHandlerProvider = koinInject<ImageHandlerProvider>()

    ObserveAsEvents(flow = viewModel.uiEvent) { event ->
        when (event) {
            is CompleteUiEvent.NavigateBack -> {
                onNavigateBack()
            }

            is CompleteUiEvent.SaveBandalart -> {
                imageHandlerProvider.saveUriToGallery(event.imageUri)
                scope.launch {
                    showToast(getString(Res.string.save_bandalart_image))
                }
            }

            is CompleteUiEvent.ShareBandalart -> {
                imageHandlerProvider.shareImage(event.imageUri)
            }
        }
    }

    CompleteScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
internal fun CompleteScreen(
    uiState: CompleteUiState,
    onAction: (CompleteUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // val configuration = LocalConfiguration.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Gray50,
    ) {
        Box {
            LottieImage(
                jsonString = FINISH_LOTTIE_FILE,
                iterations = Int.MAX_VALUE,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopCenter),
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                CompleteTopBar(onNavigateBack = { onAction(CompleteUiAction.OnBackButtonClick) })
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = stringResource(Res.string.complete_title),
                    modifier = modifier,
                    color = Gray900,
                    fontFamily = pretendardFontFamily(),
                    fontWeight = FontWeight.W700,
                    fontSize = 22.sp,
                    lineHeight = 30.8.sp,
                    textAlign = TextAlign.Center,
                )
                Box(modifier = Modifier.fillMaxSize()) {
                    CompleteBandalart(
                        profileEmoji = uiState.profileEmoji,
                        title = uiState.title,
                        // bandalartChartImageUri = uiState.bandalartChartImageUri,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    Column(
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
                        BandalartButton(
                            onClick = { onAction(CompleteUiAction.OnSaveButtonClick) },
                            text = stringResource(Res.string.complete_save),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                                .clip(shape = RoundedCornerShape(50.dp))
                                .background(Gray900),
                        )
                        BandalartButton(
                            onClick = { onAction(CompleteUiAction.OnShareButtonClick) },
                            text = stringResource(Res.string.complete_share),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
                                .clip(shape = RoundedCornerShape(50.dp))
                                .background(Gray900),
                        )
                    }
                }
            }
        }
//        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            Box {
//                Column(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .verticalScroll(rememberScrollState()),
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                ) {
//                    Spacer(modifier = Modifier.height(16.dp))
//                    CompleteTopBar(onNavigateBack = { onAction(CompleteUiAction.OnBackButtonClick) })
//                    Text(
//                        text = stringResource(Res.string.complete_title),
//                        modifier = modifier,
//                        color = Gray900,
//                        fontFamily = pretendard,
//                        fontWeight = FontWeight.W700,
//                        fontSize = 22.sp,
//                        lineHeight = 30.8.sp,
//                        textAlign = TextAlign.Center,
//                    )
//                    Spacer(modifier = Modifier.height(32.dp))
//                    Text(text = "🥳", fontSize = 100.sp)
//                    Spacer(modifier = Modifier.height(32.dp))
//                    CompleteBandalart(
//                        profileEmoji = uiState.profileEmoji,
//                        title = uiState.title,
//                        // bandalartChartImageUri = uiState.bandalartChartImageUri,
//                        modifier = Modifier.width(328.dp),
//                    )
//                    Spacer(modifier = Modifier.height(32.dp))
//                    // MVP 제외
//                    // SaveImageButton(modifier = Modifier.align(Alignment.BottomCenter))
//                    BandalartButton(
//                        onClick = { onAction(CompleteUiAction.OnShareButtonClick) },
//                        text = stringResource(Res.string.complete_share),
//                        modifier = Modifier
//                            .width(328.dp)
//                            .padding(bottom = 32.dp),
//                    )
//                }
//            }
//        } else {
//            Box {
//                LottieImage(
//                    jsonString = FINISH_LOTTIE_FILE,
//                    iterations = Int.MAX_VALUE,
//                    modifier = Modifier.align(Alignment.TopCenter),
//                )
//                Column(
//                    modifier = Modifier.fillMaxSize(),
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                ) {
//                    Spacer(modifier = Modifier.height(16.dp))
//                    CompleteTopBar(onNavigateBack = { onAction(CompleteUiAction.OnBackButtonClick) })
//                    Spacer(modifier = Modifier.height(40.dp))
//                    Text(
//                        text = stringResource(Res.string.complete_title),
//                        modifier = modifier,
//                        color = Gray900,
//                        fontFamily = pretendard,
//                        fontWeight = FontWeight.W700,
//                        fontSize = 22.sp,
//                        lineHeight = 30.8.sp,
//                        textAlign = TextAlign.Center,
//                    )
//                    Box(modifier = Modifier.fillMaxSize()) {
//                        CompleteBandalart(
//                            profileEmoji = uiState.profileEmoji,
//                            title = uiState.title,
//                            // bandalartChartImageUri = uiState.bandalartChartImageUri,
//                            modifier = Modifier.align(Alignment.Center),
//                        )
//                        Column(
//                            modifier = Modifier.align(Alignment.BottomCenter),
//                        ) {
//                            BandalartButton(
//                                onClick = { onAction(CompleteUiAction.OnSaveButtonClick) },
//                                text = stringResource(Res.string.complete_save),
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(horizontal = 24.dp, vertical = 8.dp)
//                                    .clip(shape = RoundedCornerShape(50.dp))
//                                    .background(Gray900),
//                            )
//                            BandalartButton(
//                                onClick = { onAction(CompleteUiAction.OnShareButtonClick) },
//                                text = stringResource(Res.string.complete_share),
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(top = 8.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
//                                    .clip(shape = RoundedCornerShape(50.dp))
//                                    .background(Gray900),
//                            )
//                        }
//                    }
//                }
//            }
    }
}

// @DevicePreview
// @Composable
// private fun CompleteScreenPreview() {
//     BandalartTheme {
//         CompleteScreen(
//             uiState = CompleteUiState(
//                 id = 0L,
//                 title = "발전하는 예진",
//                 profileEmoji = "😎",
//             ),
//             onAction = {},
//         )
//     }
// }
