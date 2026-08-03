package com.nexters.bandalart.feature.home

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration.Indefinite
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.nexters.bandalart.core.common.extension.await
import com.nexters.bandalart.core.ui.R
import com.nexters.bandalart.feature.home.HomeScreen.Event
import com.nexters.bandalart.feature.home.HomeScreen.State
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("TooGenericExceptionCaught")
@Composable
internal fun HandleAppUpdate(
    state: State,
    snackbarHostState: SnackbarHostState,
    eventSink: (Event) -> Unit,
    appUpdateManager: AppUpdateManager? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val updateManager = remember(context, appUpdateManager) {
        appUpdateManager ?: AppUpdateManagerFactory.create(context)
    }

    val installStateUpdatedListener = remember(updateManager, snackbarHostState, context, scope) {
        InstallStateUpdatedListener { installState ->
            if (installState.installStatus() == InstallStatus.DOWNLOADED) {
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.update_ready_to_install),
                        actionLabel = context.getString(R.string.update_action_restart),
                        duration = Indefinite,
                    )

                    if (result == SnackbarResult.ActionPerformed) {
                        updateManager.completeUpdate()
                    }
                }
            }
        }
    }

    DisposableEffect(updateManager, installStateUpdatedListener) {
        updateManager.registerListener(installStateUpdatedListener)
        onDispose {
            updateManager.unregisterListener(installStateUpdatedListener)
        }
    }

    val appUpdateResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_CANCELED) {
            eventSink(Event.OnUpdateCanceled)
        }
    }

    // 업데이트 체크
    LaunchedEffect(Unit) {
        try {
            val appUpdateInfo = updateManager.appUpdateInfo.await()
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                eventSink(Event.OnUpdateCheck(appUpdateInfo.availableVersionCode()))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check for flexible update")
        }
    }

    // 업데이트 실행
    LaunchedEffect(state.updateVersionCode) {
        state.updateVersionCode?.let {
            try {
                // 업데이트 Flow 시작
                val appUpdateInfo = updateManager.appUpdateInfo.await()
                updateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    appUpdateResultLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to start update flow")
            }
        }
    }
}
