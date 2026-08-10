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

package com.nexters.bandalart.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader
import com.nexters.bandalart.R
import com.nexters.bandalart.core.common.RewardedAdGateway
import com.nexters.bandalart.core.common.RewardedAdResult
import java.lang.ref.WeakReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AndroidRewardedAdGateway(
    application: Application,
    private val awaitAdsInitialized: suspend () -> Boolean,
    private val recordReward: suspend (Long) -> Boolean,
) : RewardedAdGateway,
    Application.ActivityLifecycleCallbacks {
    private val application = application
    private val mainHandler = Handler(Looper.getMainLooper())
    private val recordingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var resumedActivity = WeakReference<Activity>(null)
    private val requests = mutableMapOf<Long, CompletableDeferred<RewardedAdResult>>()
    private val pendingLoadedAds = mutableMapOf<CompletableDeferred<RewardedAdResult>, PendingLoadedAd>()

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    override suspend fun show(requestId: Long): RewardedAdResult =
        withContext(Dispatchers.Main.immediate) {
            if (!awaitAdsInitialized()) return@withContext RewardedAdResult.FAILED
            requests
                .getOrPut(requestId) {
                    CompletableDeferred<RewardedAdResult>().also { result -> load(requestId, result) }
                }.await()
        }

    override fun consume(requestId: Long) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            requests.remove(requestId)
        } else {
            application.mainExecutor.execute { requests.remove(requestId) }
        }
    }

    private fun load(
        requestId: Long,
        result: CompletableDeferred<RewardedAdResult>,
    ) {
        if (resumedActivity.get() == null) {
            result.complete(RewardedAdResult.FAILED)
            return
        }

        fun finish(rewardedAdResult: RewardedAdResult) {
            result.complete(rewardedAdResult)
        }

        val adRequest =
            AdRequest
                .Builder(application.getString(R.string.admob_rewarded_ad_unit_id))
                .setGoogleExtrasBundle(nonPersonalizedAdExtras())
                .build()
        val preloadedAd = RewardedAdPreloader.pollAd(adRequest.adUnitId)
        if (preloadedAd != null) {
            showWhenActivityAvailable(requestId, preloadedAd, result)
            return
        }
        RewardedAd.load(
            adRequest,
            object : AdLoadCallback<RewardedAd> {
                override fun onAdLoaded(ad: RewardedAd) {
                    mainHandler.post { showWhenActivityAvailable(requestId, ad, result) }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mainHandler.post { finish(RewardedAdResult.FAILED) }
                }
            },
        )
    }

    private fun showWhenActivityAvailable(
        requestId: Long,
        ad: RewardedAd,
        result: CompletableDeferred<RewardedAdResult>,
    ) {
        val activity = resumedActivity.get()
        if (activity != null) {
            show(requestId, ad, activity, result::complete)
            return
        }

        pendingLoadedAds[result] = PendingLoadedAd(requestId, ad)
        mainHandler.postDelayed(
            {
                if (pendingLoadedAds.remove(result) != null) {
                    result.complete(RewardedAdResult.FAILED)
                }
            },
            ACTIVITY_REATTACH_GRACE_PERIOD_MILLIS,
        )
    }

    private fun show(
        requestId: Long,
        ad: RewardedAd,
        activity: Activity,
        finish: (RewardedAdResult) -> Unit,
    ) {
        val callbackCoordinator =
            RewardedAdCallbackCoordinator(
                finish = finish,
                scheduleDismissed = { action ->
                    mainHandler.postDelayed(action, DISMISS_CALLBACK_GRACE_PERIOD_MILLIS)
                },
            )
        ad.adEventCallback =
            object : RewardedAdEventCallback {
                override fun onAdDismissedFullScreenContent() {
                    mainHandler.post { callbackCoordinator.onDismissed() }
                }

                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                    mainHandler.post { callbackCoordinator.onFailed() }
                }
            }
        ad.show(activity) {
            mainHandler.post { callbackCoordinator.onRewardRecordingStarted() }
            recordingScope.launch(start = CoroutineStart.UNDISPATCHED) {
                val recorded = runCatching { recordReward(requestId) }.getOrDefault(false)
                withContext(Dispatchers.Main.immediate) {
                    if (recorded) {
                        callbackCoordinator.onRewardEarned()
                    } else {
                        callbackCoordinator.onFailed()
                    }
                }
            }
        }
    }

    override fun onActivityResumed(activity: Activity) {
        resumedActivity = WeakReference(activity)
        val adsToShow = pendingLoadedAds.toMap()
        pendingLoadedAds.clear()
        adsToShow.forEach { (result, pendingAd) ->
            if (!result.isCompleted) {
                show(pendingAd.requestId, pendingAd.ad, activity, result::complete)
            }
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumedActivity.get() === activity) resumedActivity.clear()
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

    private companion object {
        const val DISMISS_CALLBACK_GRACE_PERIOD_MILLIS = 1_000L
        const val ACTIVITY_REATTACH_GRACE_PERIOD_MILLIS = 2_000L
    }

    private data class PendingLoadedAd(
        val requestId: Long,
        val ad: RewardedAd,
    )
}

internal class RewardedAdCallbackCoordinator(
    private val finish: (RewardedAdResult) -> Unit,
    private val scheduleDismissed: (() -> Unit) -> Unit,
) {
    private var rewardEarned = false
    private var rewardRecording = false
    private var dismissed = false
    private var finished = false

    fun onRewardRecordingStarted() {
        rewardRecording = true
    }

    fun onRewardEarned() {
        rewardRecording = false
        rewardEarned = true
        if (dismissed) finishOnce(RewardedAdResult.REWARDED)
    }

    fun onDismissed() {
        dismissed = true
        if (rewardEarned) {
            finishOnce(RewardedAdResult.REWARDED)
        } else {
            scheduleDismissed {
                when {
                    rewardEarned -> finishOnce(RewardedAdResult.REWARDED)
                    rewardRecording -> onDismissed()
                    else -> finishOnce(RewardedAdResult.DISMISSED)
                }
            }
        }
    }

    fun onFailed() {
        rewardRecording = false
        finishOnce(RewardedAdResult.FAILED)
    }

    private fun finishOnce(result: RewardedAdResult) {
        if (finished) return
        finished = true
        finish(result)
    }
}
