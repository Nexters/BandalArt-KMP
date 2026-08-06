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

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.update_action_restart
import bandalart.core.designsystem.generated.resources.update_install_failed
import bandalart.core.designsystem.generated.resources.update_installing
import bandalart.core.designsystem.generated.resources.update_ready_to_install
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.nexters.bandalart.core.common.utils.isImmediateUpdate
import io.github.aakira.napier.Napier
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.compose.resources.getString
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
internal actual fun FlexibleUpdateEffect(
    updateVersionCode: Int?,
    snackbarHostState: SnackbarHostState,
    onUpdateAvailable: (Int) -> Unit,
    onUpdateCanceled: () -> Unit,
) {
    FlexibleUpdateEffect(
        updateVersionCode = updateVersionCode,
        snackbarHostState = snackbarHostState,
        onUpdateAvailable = onUpdateAvailable,
        onUpdateCanceled = onUpdateCanceled,
        appUpdateManager = null,
    )
}

@Suppress("TooGenericExceptionCaught")
@Composable
internal fun FlexibleUpdateEffect(
    updateVersionCode: Int?,
    snackbarHostState: SnackbarHostState,
    onUpdateAvailable: (Int) -> Unit,
    onUpdateCanceled: () -> Unit,
    appUpdateManager: AppUpdateManager?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val currentOnUpdateAvailable by rememberUpdatedState(onUpdateAvailable)
    val currentOnUpdateCanceled by rememberUpdatedState(onUpdateCanceled)
    val updateManager =
        remember(context, appUpdateManager) {
            appUpdateManager ?: AppUpdateManagerFactory.create(context)
        }

    val installStateUpdatedListener =
        remember(updateManager, snackbarHostState, scope) {
            InstallStateUpdatedListener { installState ->
                if (installState.installStatus() == InstallStatus.DOWNLOADED) {
                    scope.launch {
                        promptForCompleteUpdate(snackbarHostState, updateManager)
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

    val updateResultLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_CANCELED) {
                currentOnUpdateCanceled()
            }
        }

    LaunchedEffect(lifecycleState, updateManager) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            try {
                val appUpdateInfo = updateManager.appUpdateInfo.await()
                when {
                    appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED -> {
                        promptForCompleteUpdate(snackbarHostState, updateManager)
                    }

                    appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) &&
                        !isImmediateUpdate(context, appUpdateInfo.availableVersionCode()) -> {
                        currentOnUpdateAvailable(appUpdateInfo.availableVersionCode())
                    }
                }
            } catch (exception: Exception) {
                Napier.e("Failed to check flexible update status", exception, tag = "InAppUpdate")
            }
        }
    }

    LaunchedEffect(updateVersionCode, updateManager) {
        val requestedVersionCode = updateVersionCode ?: return@LaunchedEffect
        try {
            val appUpdateInfo = updateManager.appUpdateInfo.await()
            val canStartUpdate =
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    appUpdateInfo.availableVersionCode() == requestedVersionCode &&
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) &&
                    !isImmediateUpdate(context, requestedVersionCode)

            if (canStartUpdate) {
                updateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateResultLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                )
            }
        } catch (exception: Exception) {
            Napier.e("Failed to start flexible update", exception, tag = "InAppUpdate")
        }
    }
}

private fun isImmediateUpdate(
    context: Context,
    availableVersionCode: Int,
): Boolean {
    val currentVersionCode =
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .longVersionCode
            .toInt()
    return isImmediateUpdate(
        currentVersionCode = currentVersionCode,
        availableVersionCode = availableVersionCode,
    )
}

@Suppress("TooGenericExceptionCaught")
private suspend fun promptForCompleteUpdate(
    snackbarHostState: SnackbarHostState,
    updateManager: AppUpdateManager,
) {
    var message = getString(Res.string.update_ready_to_install)

    while (true) {
        val result =
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = getString(Res.string.update_action_restart),
                duration = SnackbarDuration.Indefinite,
            )
        if (result != SnackbarResult.ActionPerformed) return

        try {
            coroutineScope {
                launch {
                    snackbarHostState.showSnackbar(
                        message = getString(Res.string.update_installing),
                        duration = SnackbarDuration.Indefinite,
                    )
                }
                updateManager.completeUpdate().awaitCompletion()
            }
            return
        } catch (exception: Exception) {
            Napier.e("Failed to complete flexible update", exception, tag = "InAppUpdate")
            message = getString(Res.string.update_install_failed)
        }
    }
}

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener { exception ->
            if (continuation.isActive) continuation.resumeWithException(exception)
        }
    }

private suspend fun Task<Void>.awaitCompletion() {
    await()
}
