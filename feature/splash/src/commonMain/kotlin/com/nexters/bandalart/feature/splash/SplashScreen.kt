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
import androidx.navigation.NavOptions
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.app_icon_description
import bandalart.core.designsystem.generated.resources.ic_app
import com.nexters.bandalart.core.common.utils.ObserveAsEvents
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.designsystem.theme.Gray50
import com.nexters.bandalart.core.navigation.Route
import com.nexters.bandalart.core.ui.component.AppTitle
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Suppress("TooGenericExceptionCaught")
@Composable
internal fun SplashRoute(
    navigateToOnBoarding: (NavOptions) -> Unit,
    navigateToHome: (NavOptions) -> Unit,
    viewModel: SplashViewModel = koinViewModel(),
) {
//    val context = LocalContext.current
//    val appUpdateManager: AppUpdateManager = remember { AppUpdateManagerFactory.create(context) }
//    val lifecycle = LocalLifecycleOwner.current.lifecycle
//    val lifecycleState by lifecycle.currentStateFlow.collectAsStateWithLifecycle()

//    val appUpdateResultLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.StartIntentSenderForResult(),
//    ) { result ->
//        if (result.resultCode == Activity.RESULT_CANCELED) {
//            activity.finish()
//        }
//    }
//
//    LaunchedEffect(Unit) {
//        try {
//            val appUpdateInfo = appUpdateManager.appUpdateInfo.await()
//
//            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
//                val availableVersionCode = appUpdateInfo.availableVersionCode()
//
//                if (isValidImmediateAppUpdate(availableVersionCode) &&
//                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
//                ) {
//                    appUpdateManager.startUpdateFlowForResult(
//                        appUpdateInfo,
//                        appUpdateResultLauncher,
//                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
//                    )
//                } else {
//                    viewModel.checkOnboardingStatus()
//                }
//            } else {
//                viewModel.checkOnboardingStatus()
//            }
//        } catch (e: Exception) {
//            Napier.e( "Failed to check for immediate update", e)
//            viewModel.checkOnboardingStatus()
//        }
//    }
//
//    // LifecycleResumeEffect 는 내부에 suspend 함수를 사용할 수 없다.
//    LaunchedEffect(lifecycleState) {
//        if (lifecycleState == Lifecycle.State.RESUMED) {
//            try {
//                val appUpdateInfo = appUpdateManager.appUpdateInfo.await()
//                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
//                    appUpdateManager.startUpdateFlowForResult(
//                        appUpdateInfo,
//                        appUpdateResultLauncher,
//                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
//                    )
//                }
//            } catch (e: Exception) {
//                Napier.e("Failed to check update status on resume", e)
//            }
//        }
//    }

    ObserveAsEvents(flow = viewModel.uiEvent) { event ->
        when (event) {
            is SplashUiEvent.NavigateToOnBoarding -> {
                val options = NavOptions.Builder()
                    .setPopUpTo(Route.Splash, inclusive = true)
                    .build()
                navigateToOnBoarding(options)
            }

            is SplashUiEvent.NavigateToHome -> {
                val options = NavOptions.Builder()
                    .setPopUpTo(Route.Splash, inclusive = true)
                    .build()
                navigateToHome(options)
            }
        }
    }

    SplashScreen()
}

@Composable
internal fun SplashScreen(
    modifier: Modifier = Modifier,
) {
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

// @DevicePreview
@Preview
@Composable
private fun SplashScreenPreview() {
    BandalartTheme {
        SplashScreen()
    }
}
