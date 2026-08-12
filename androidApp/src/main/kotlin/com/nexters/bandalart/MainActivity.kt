/*
 * Copyright 2025 easyhooon
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

package com.nexters.bandalart

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.nexters.bandalart.core.common.utils.isMandatoryUpdate
import io.github.aakira.napier.Napier
import com.nexters.bandalart.widget.BandalartWidgetLaunchRequest
import com.nexters.bandalart.di.metro.recordAndroidWidgetLaunch

class MainActivity : ComponentActivity() {
    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var updateResultLauncher: ActivityResultLauncher<IntentSenderRequest>
    private var skipNextResumeForSplash = false
    private var isUpdateCheckInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            (application as BandalartApplication).reconcileDeadlineRemindersOnUserLaunch()
        }
        recordWidgetLaunch(intent)
        appUpdateManager = AppUpdateManagerFactory.create(this)
        skipNextResumeForSplash = savedInstanceState == null
        updateResultLauncher =
            registerForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult(),
            ) { result ->
                if (result.resultCode == Activity.RESULT_CANCELED) {
                    finish()
                }
            }
        enableEdgeToEdge()
        setContent {
            BandalartApp(
                appGraph = (application as BandalartApplication).appGraph,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recordWidgetLaunch(intent)
    }

    private fun recordWidgetLaunch(intent: Intent?) {
        val request =
            BandalartWidgetLaunchRequest.from(
                action = intent?.action,
                bandalartId =
                    intent?.getLongExtra(BandalartWidgetLaunchRequest.EXTRA_BANDALART_ID, -1L)
                        ?: -1L,
            ) ?: return
        recordAndroidWidgetLaunch(
            appGraph = (application as BandalartApplication).appGraph,
            bandalartId = request.bandalartId,
        )
    }

    override fun onResume() {
        super.onResume()
        if (skipNextResumeForSplash) {
            skipNextResumeForSplash = false
            return
        }
        checkForImmediateUpdate()
    }

    private fun checkForImmediateUpdate() {
        if (isUpdateCheckInProgress) return
        isUpdateCheckInProgress = true

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                isUpdateCheckInProgress = false
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    return@addOnSuccessListener
                }
                when {
                    appUpdateInfo.updateAvailability() ==
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        startImmediateUpdate(appUpdateInfo)
                    }

                    appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) &&
                        isMandatoryUpdate(appUpdateInfo.updatePriority()) -> {
                        startImmediateUpdate(appUpdateInfo)
                    }
                }
            }.addOnFailureListener { exception ->
                isUpdateCheckInProgress = false
                Napier.e(
                    "Failed to check immediate update on foreground",
                    exception,
                    tag = "InAppUpdate",
                )
            }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun startImmediateUpdate(appUpdateInfo: AppUpdateInfo) {
        try {
            val started =
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateResultLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                )
            if (!started) {
                Napier.w("Immediate update flow was not started", tag = "InAppUpdate")
            }
        } catch (exception: Exception) {
            Napier.e(
                "Failed to start immediate update on foreground",
                exception,
                tag = "InAppUpdate",
            )
        }
    }
}
