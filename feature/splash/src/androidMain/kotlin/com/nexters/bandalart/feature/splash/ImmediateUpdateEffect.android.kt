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

package com.nexters.bandalart.feature.splash

import android.app.Activity
import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import io.github.aakira.napier.Napier
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Suppress("TooGenericExceptionCaught")
@Composable
internal actual fun ImmediateUpdateEffect(onComplete: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val appUpdateManager = remember(context) { AppUpdateManagerFactory.create(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val currentOnComplete by rememberUpdatedState(onComplete)

    val updateResultLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_CANCELED) {
                activity?.finish()
            }
        }

    LaunchedEffect(appUpdateManager) {
        try {
            val appUpdateInfo = appUpdateManager.appUpdateInfo.await()
            val canStartImmediateUpdate =
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    isValidImmediateUpdate(context, appUpdateInfo.availableVersionCode()) &&
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)

            if (canStartImmediateUpdate) {
                appUpdateManager.startImmediateUpdate(appUpdateInfo, updateResultLauncher)
            } else {
                currentOnComplete()
            }
        } catch (exception: Exception) {
            Napier.e("Failed to check for immediate update", exception, tag = "InAppUpdate")
            currentOnComplete()
        }
    }

    LaunchedEffect(lifecycleState, appUpdateManager) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            try {
                val appUpdateInfo = appUpdateManager.appUpdateInfo.await()
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    appUpdateManager.startImmediateUpdate(appUpdateInfo, updateResultLauncher)
                }
            } catch (exception: Exception) {
                Napier.e("Failed to resume immediate update", exception, tag = "InAppUpdate")
            }
        }
    }
}

private fun isValidImmediateUpdate(
    context: Context,
    availableVersionCode: Int,
): Boolean {
    val currentVersionCode =
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .longVersionCode
            .toInt()
    val availableMajor = availableVersionCode / 10_000
    val availableMinor = (availableVersionCode % 10_000) / 100
    val currentMajor = currentVersionCode / 10_000
    val currentMinor = (currentVersionCode % 10_000) / 100

    return availableMajor > currentMajor || availableMinor > currentMinor
}

private fun AppUpdateManager.startImmediateUpdate(
    appUpdateInfo: AppUpdateInfo,
    launcher: ActivityResultLauncher<IntentSenderRequest>,
) {
    startUpdateFlowForResult(
        appUpdateInfo,
        launcher,
        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
    )
}

private suspend fun Task<AppUpdateInfo>.await(): AppUpdateInfo =
    suspendCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("App update check failed"),
                )
            }
        }
    }
