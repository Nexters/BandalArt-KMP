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

import android.app.Application
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.google.firebase.Firebase
import com.google.firebase.initialize
import com.nexters.bandalart.ads.AdsInitializer
import com.nexters.bandalart.ads.AndroidBannerAdHost
import com.nexters.bandalart.ads.AndroidRewardedAdGateway
import com.nexters.bandalart.ads.DelegatingRewardedAdGateway
import com.nexters.bandalart.di.metro.AppGraph
import com.nexters.bandalart.di.metro.createAndroidAppGraph
import com.nexters.bandalart.di.metro.installAndroidDeadlineReminderInfrastructure
import com.nexters.bandalart.di.metro.observeAndroidWidgetBandalarts
import com.nexters.bandalart.di.metro.observeAndroidWidgetRecentBandalartId
import com.nexters.bandalart.di.metro.reconcileAndroidDeadlineReminders
import com.nexters.bandalart.di.metro.recordRewardedCreation
import com.nexters.bandalart.widget.BandalartGlanceWidget
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.github.easyhooon.ding.Ding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class BandalartApplication : Application() {
    lateinit var appGraph: AppGraph
        private set
    private val adsInitializer by lazy { AdsInitializer(applicationContext) }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        val rewardedAdGateway = DelegatingRewardedAdGateway()
        val bannerAdHost = AndroidBannerAdHost(adsInitializer::awaitInitialized)
        appGraph =
            createAndroidAppGraph(
                application = this,
                bannerAdHost = bannerAdHost,
                rewardedAdGateway = rewardedAdGateway,
            )
        installAndroidDeadlineReminderInfrastructure(
            appGraph = appGraph,
            launchIntentFactory = { _, _ ->
                Intent(this, DeadlineNotificationTrampolineActivity::class.java)
            },
            notificationCapture = { notificationId, title, body, data ->
                Ding.captureNotification(
                    context = this,
                    source = "deadline-reminder",
                    notificationId = notificationId,
                    title = title,
                    body = body,
                    data = data,
                )
            },
        )
        rewardedAdGateway.delegate =
            AndroidRewardedAdGateway(
                application = this,
                awaitAdsInitialized = adsInitializer::awaitInitialized,
                recordReward = { requestId ->
                    recordRewardedCreation(appGraph, requestId)
                },
            )

        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }

        Firebase.initialize(this)
        adsInitializer.initialize()

        multiplatform.network.cmptoast.AppContext
            .apply { set(applicationContext) }

        observeBandalartChangesForWidgets()
    }

    internal fun reconcileDeadlineRemindersOnUserLaunch() {
        applicationScope.launch {
            reconcileAndroidDeadlineReminders(appGraph)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun observeBandalartChangesForWidgets() {
        applicationScope.launch {
            combine(
                observeAndroidWidgetBandalarts(appGraph),
                observeAndroidWidgetRecentBandalartId(appGraph),
            ) { _, _ -> Unit }.collect {
                try {
                    BandalartGlanceWidget().updateAll(this@BandalartApplication)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Napier.e("Failed to refresh widgets after an app edit", exception, tag = "BandalartWidget")
                }
            }
        }
    }
}
