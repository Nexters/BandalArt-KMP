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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bandalart.core.designsystem.generated.resources.app_version_info
import bandalart.core.designsystem.generated.resources.save_bandalart_image
import bandalart.core.designsystem.generated.resources.Res
import com.nexters.bandalart.core.common.AppVersionProvider
import com.nexters.bandalart.core.common.ImageHandlerProvider
import com.nexters.bandalart.core.common.extension.captureToGraphicsLayer
import com.nexters.bandalart.core.common.utils.ObserveAsEvents
import com.nexters.bandalart.core.designsystem.theme.Gray100
import com.nexters.bandalart.core.designsystem.theme.Gray50
import com.nexters.bandalart.feature.home.ui.HomeHeader
import com.nexters.bandalart.feature.home.ui.HomeShareButton
import com.nexters.bandalart.feature.home.ui.HomeTopBar
import com.nexters.bandalart.feature.home.ui.bandalart.BandalartChart
import com.nexters.bandalart.feature.home.ui.bandalart.BandalartSkeleton
import com.nexters.bandalart.feature.home.viewmodel.HomeUiAction
import com.nexters.bandalart.feature.home.viewmodel.HomeUiEvent
import com.nexters.bandalart.feature.home.viewmodel.HomeUiState
import com.nexters.bandalart.feature.home.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private const val SnackbarDuration = 1500L

// TODO 서브 셀을 먼저 채워야 태스크 셀을 채울 수 있도록 validation 추가
// TODO 텍스트를 컴포저블로 각각 분리하지 말고, 폰트를 적용하는 방식으로 변경
@Suppress("TooGenericExceptionCaught")
@Composable
internal fun HomeRoute(
    navigateToComplete: (Long, String, String, String) -> Unit,
    onShowSnackbar: suspend (String) -> Boolean,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val appVersionProvider = koinInject<AppVersionProvider>()
    val imageHandlerProvider = koinInject<ImageHandlerProvider>()
    val appVersion = remember {
        appVersionProvider.getAppVersion()
    }

//    val appUpdateManager = remember { AppUpdateManagerFactory.create(context) }
//
//    val installStateUpdatedListener = remember {
//        InstallStateUpdatedListener { state ->
//            if (state.installStatus() == InstallStatus.DOWNLOADED) {
//                scope.launch {
//                    val snackbarResult = snackbarHostState.showSnackbar(
//                        message = context.getString(R.string.update_ready_to_install),
//                        actionLabel = context.getString(R.string.update_action_restart),
//                        duration = Indefinite,
//                    )
//
//                    // 재시작 버튼 클릭시
//                    if (snackbarResult == SnackbarResult.ActionPerformed) {
//                        appUpdateManager.completeUpdate()
//                    }
//                }
//            }
//        }
//    }

//    DisposableEffect(Unit) {
//        appUpdateManager.registerListener(installStateUpdatedListener)
//        onDispose {
//            appUpdateManager.unregisterListener(installStateUpdatedListener)
//        }
//    }
//
//    val appUpdateResultLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.StartIntentSenderForResult(),
//    ) { result ->
//        if (result.resultCode == Activity.RESULT_CANCELED && result.data != null) {
//            scope.launch {
//                appUpdateManager.appUpdateInfo.await().availableVersionCode().let { versionCode ->
//                    viewModel.setLastRejectedUpdateVersion(versionCode)
//                }
//            }
//        }
//    }

//    LaunchedEffect(Unit) {
//        try {
//            val appUpdateInfo = appUpdateManager.appUpdateInfo.await()
//
//            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
//                val availableVersionCode = appUpdateInfo.availableVersionCode()
//                if (!isValidImmediateAppUpdate(availableVersionCode) &&
//                    !viewModel.isUpdateAlreadyRejected(availableVersionCode) &&
//                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
//                ) {
//                    appUpdateManager.startUpdateFlowForResult(
//                        appUpdateInfo,
//                        appUpdateResultLauncher,
//                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
//                    )
//                }
//            }
//        } catch (e: Exception) {
//            Napier.e("Failed to check for flexible update", e)
//        }
//    }

    ObserveAsEvents(flow = viewModel.uiEvent) { event ->
        when (event) {
            is HomeUiEvent.NavigateToComplete -> {
                navigateToComplete(
                    event.id,
                    event.title,
                    event.profileEmoji.ifEmpty { "default emoji" },
                    event.bandalartChart,
                )
            }

            is HomeUiEvent.ShowSnackbar -> {
                scope.launch {
                    val job = launch {
                        onShowSnackbar(event.message)
                    }
                    delay(SnackbarDuration)
                    job.cancel()
                }
            }

            is HomeUiEvent.ShowToast -> {
                scope.launch {
                    showToast(event.message)
                }
            }

            is HomeUiEvent.SaveBandalart -> {
                imageHandlerProvider.saveImageToGallery(event.bitmap)
                scope.launch {
                    showToast(getString(Res.string.save_bandalart_image))
                }
            }

            is HomeUiEvent.ShareBandalart -> {
                imageHandlerProvider.externalShareForBitmap(event.bitmap)
            }

            is HomeUiEvent.CaptureBandalart -> {
                viewModel.updateBandalartChartUrl(imageHandlerProvider.bitmapToFileUri(event.bitmap).toString())
            }

            is HomeUiEvent.ShowAppVersion -> {
                scope.launch {
                    showToast(getString(Res.string.app_version_info, appVersion))
                }
            }
        }
    }

    HomeScreen(
        uiState = uiState,
        onHomeUiAction = viewModel::onAction,
        shareBandalart = viewModel::shareBandalart,
        captureBandalart = viewModel::captureBandalart,
        saveBandalart = viewModel::saveBandalartImage,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onHomeUiAction: (HomeUiAction) -> Unit,
    shareBandalart: (ImageBitmap) -> Unit,
    captureBandalart: (ImageBitmap) -> Unit,
    saveBandalart: (ImageBitmap) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val homeGraphicsLayer = rememberGraphicsLayer()
    val completeGraphicsLayer = rememberGraphicsLayer()

    LaunchedEffect(key1 = uiState.isSharing) {
        if (uiState.isSharing) {
            shareBandalart(homeGraphicsLayer.toImageBitmap())
        }
    }

    LaunchedEffect(key1 = uiState.isCapturing) {
        if (uiState.isCapturing) {
            if (uiState.isBandalartCompleted) {
                captureBandalart(completeGraphicsLayer.toImageBitmap())
            } else {
                saveBandalart(completeGraphicsLayer.toImageBitmap())
            }
        }
    }

    HomeBottomSheets(
        uiState = uiState,
        onHomeUiAction = onHomeUiAction,
    )

    HomeDialogs(
        uiState = uiState,
        onHomeUiAction = onHomeUiAction,
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Gray50,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
            ) {
                HomeTopBar(
                    bandalartCount = uiState.bandalartList.size,
                    onHomeUiAction = onHomeUiAction,
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Gray100,
                )
                Column(
                    modifier = Modifier
                        .captureToGraphicsLayer(homeGraphicsLayer)
                        .background(Gray50),
                ) {
                    if (uiState.bandalartCellData != null && uiState.bandalartData != null) {
                        HomeHeader(
                            bandalartData = uiState.bandalartData,
                            cellData = uiState.bandalartCellData,
                            isDropDownMenuOpened = uiState.isDropDownMenuOpened,
                            onHomeUiAction = onHomeUiAction,
                        )
                        BandalartChart(
                            bandalartData = uiState.bandalartData,
                            bandalartCellData = uiState.bandalartCellData,
                            onHomeUiAction = onHomeUiAction,
                            modifier = Modifier
                                .captureToGraphicsLayer(completeGraphicsLayer)
                                .background(Gray50),
                        )
                    }
                    Spacer(modifier = Modifier.height(64.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                HomeShareButton(
                    onShareButtonClick = { onHomeUiAction(HomeUiAction.OnShareButtonClick) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            if (uiState.isShowSkeleton) {
                BandalartSkeleton()
            }
        }
    }
}

// @DevicePreview
// @Composable
// private fun HomeScreenSingleBandalartPreview() {
//     BandalartTheme {
//         HomeScreen(
//             uiState = HomeUiState(
//                 bandalartList = listOf(dummyBandalartList[0]).toImmutableList(),
//                 bandalartData = dummyBandalartData,
//                 bandalartCellData = dummyBandalartChartData,
//             ),
//             onHomeUiAction = {},
//             shareBandalart = {},
//             captureBandalart = {},
//             saveBandalart = {},
//             snackbarHostState = remember { SnackbarHostState() },
//         )
//     }
// }
//
// @DevicePreview
// @Composable
// private fun HomeScreenMultipleBandalartPreview() {
//     BandalartTheme {
//         HomeScreen(
//             uiState = HomeUiState(
//                 bandalartList = dummyBandalartList.toImmutableList(),
//                 bandalartData = dummyBandalartData,
//                 bandalartCellData = dummyBandalartChartData,
//             ),
//             onHomeUiAction = {},
//             shareBandalart = {},
//             captureBandalart = {},
//             saveBandalart = {},
//             snackbarHostState = remember { SnackbarHostState() },
//         )
//     }
// }
