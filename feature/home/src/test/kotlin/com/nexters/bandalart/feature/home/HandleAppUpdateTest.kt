package com.nexters.bandalart.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.nexters.bandalart.core.ui.R
import com.nexters.bandalart.feature.home.HomeScreen.State
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HandleAppUpdateTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun restartPromptAppearsOnlyAfterFlexibleUpdateDownloadCompletes() {
        val appUpdateManager = FakeAppUpdateManager(composeRule.activity)
        val snackbarHostState = SnackbarHostState()
        val restartLabel = composeRule.activity.getString(R.string.update_action_restart)

        appUpdateManager.setUpdateAvailable(20206, AppUpdateType.FLEXIBLE)

        composeRule.setContent {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) {
                HandleAppUpdate(
                    state = State(updateVersionCode = 20206, eventSink = {}),
                    snackbarHostState = snackbarHostState,
                    eventSink = {},
                    appUpdateManager = appUpdateManager,
                )
            }
        }

        composeRule.waitUntil { appUpdateManager.isConfirmationDialogVisible }
        composeRule.onAllNodesWithText(restartLabel).assertCountEquals(0)

        composeRule.runOnIdle {
            appUpdateManager.userAcceptsUpdate()
            appUpdateManager.downloadStarts()
            appUpdateManager.downloadCompletes()
        }

        composeRule.onNodeWithText(restartLabel).assertIsDisplayed().performClick()
        composeRule.waitUntil { appUpdateManager.isInstallSplashScreenVisible }
    }
}
