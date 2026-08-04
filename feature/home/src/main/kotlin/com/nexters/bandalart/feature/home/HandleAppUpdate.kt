package com.nexters.bandalart.feature.home

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration.Indefinite
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.tasks.Task
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsStateWithLifecycle()

    val updateManager = remember(context, appUpdateManager) {
        appUpdateManager ?: AppUpdateManagerFactory.create(context)
    }

    val installStateUpdatedListener = remember(updateManager, snackbarHostState, context, scope) {
        InstallStateUpdatedListener { installState ->
            if (installState.installStatus() == InstallStatus.DOWNLOADED) {
                scope.launch {
                    promptForCompleteUpdate(context, snackbarHostState, updateManager)
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

    // 업데이트 체크 및 다운로드 완료 상태 복구
    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            try {
                val appUpdateInfo = updateManager.appUpdateInfo.await()
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    promptForCompleteUpdate(context, snackbarHostState, updateManager)
                } else if (
                    appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    eventSink(Event.OnUpdateCheck(appUpdateInfo.availableVersionCode()))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to check flexible update status")
            }
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

@Suppress("TooGenericExceptionCaught")
private suspend fun promptForCompleteUpdate(
    context: Context,
    snackbarHostState: SnackbarHostState,
    updateManager: AppUpdateManager,
) {
    var message = context.getString(R.string.update_ready_to_install)

    while (true) {
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = context.getString(R.string.update_action_restart),
            duration = Indefinite,
        )
        if (result != SnackbarResult.ActionPerformed) return

        try {
            coroutineScope {
                launch {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.update_installing),
                        duration = Indefinite,
                    )
                }
                updateManager.completeUpdate().awaitCompletion()
                Timber.i("Flexible update completion accepted")
            }
            return
        } catch (e: Exception) {
            Timber.e(e, "Failed to complete flexible update")
            message = context.getString(R.string.update_install_failed)
        }
    }
}

private suspend fun Task<Void>.awaitCompletion() {
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener {
            if (continuation.isActive) continuation.resume(Unit)
        }
        addOnFailureListener {
            if (continuation.isActive) continuation.resumeWithException(it)
        }
    }
}
