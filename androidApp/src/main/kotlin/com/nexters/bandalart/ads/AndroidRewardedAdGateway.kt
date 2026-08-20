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
import com.nexters.bandalart.core.common.RewardedAdPurpose
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
    private val rewardPolicy = AndroidRewardedAdRewardPolicy(recordReward)
    private var resumedActivity = WeakReference<Activity>(null)
    private val requests = mutableMapOf<Long, RewardedAdRequest>()
    private val pendingLoadedAds = mutableMapOf<RewardedAdRequest, RewardedAd>()

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    override suspend fun show(
        requestId: Long,
        purpose: RewardedAdPurpose,
    ): RewardedAdResult =
        withContext(Dispatchers.Main.immediate) {
            if (!awaitAdsInitialized()) return@withContext RewardedAdResult.FAILED
            val request =
                requests[requestId]
                    ?: RewardedAdRequest(requestId, purpose).also { newRequest ->
                        requests[requestId] = newRequest
                        load(newRequest)
                    }
            request.result.await()
        }

    override fun consume(requestId: Long) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            consumeOnMain(requestId)
        } else {
            application.mainExecutor.execute { consumeOnMain(requestId) }
        }
    }

    private fun consumeOnMain(requestId: Long) {
        val request = requests[requestId] ?: return
        when (request.lifecycle.consume()) {
            RewardedAdConsumeAction.CANCEL -> {
                requests.remove(requestId, request)
                pendingLoadedAds.remove(request)
                request.result.cancel()
            }
            RewardedAdConsumeAction.ABANDON_SHOWING,
            RewardedAdConsumeAction.IGNORE,
            -> Unit
        }
    }

    private fun load(request: RewardedAdRequest) {
        if (resumedActivity.get() == null) {
            finishRequest(request, RewardedAdResult.FAILED)
            return
        }

        val adRequest =
            AdRequest
                .Builder(application.getString(request.purpose.adUnitResource))
                .setGoogleExtrasBundle(nonPersonalizedAdExtras())
                .build()
        val preloadedAd = RewardedAdPreloader.pollAd(adRequest.adUnitId)
        if (preloadedAd != null) {
            showWhenActivityAvailable(request, preloadedAd)
            return
        }
        RewardedAd.load(
            adRequest,
            object : AdLoadCallback<RewardedAd> {
                override fun onAdLoaded(ad: RewardedAd) {
                    mainHandler.post { showWhenActivityAvailable(request, ad) }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mainHandler.post { finishRequest(request, RewardedAdResult.FAILED) }
                }
            },
        )
    }

    private fun showWhenActivityAvailable(
        request: RewardedAdRequest,
        ad: RewardedAd,
    ) {
        if (!isCurrent(request)) return
        val activity = resumedActivity.get()
        if (activity != null) {
            show(request, ad, activity)
            return
        }

        if (!request.lifecycle.tryMarkPendingActivity()) return
        pendingLoadedAds[request] = ad
        mainHandler.postDelayed(
            {
                if (pendingLoadedAds.remove(request) != null) {
                    finishRequest(request, RewardedAdResult.FAILED)
                }
            },
            ACTIVITY_REATTACH_GRACE_PERIOD_MILLIS,
        )
    }

    private fun show(
        request: RewardedAdRequest,
        ad: RewardedAd,
        activity: Activity,
    ) {
        if (!isCurrent(request) || !request.lifecycle.tryMarkShowing()) return
        val callbackCoordinator =
            RewardedAdCallbackCoordinator(
                finish = { result -> finishRequest(request, result) },
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
                val recorded =
                    runCatching { rewardPolicy.complete(request.requestId, request.purpose) }
                        .getOrDefault(false)
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
        adsToShow.forEach { (request, ad) ->
            show(request, ad, activity)
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

    private fun isCurrent(request: RewardedAdRequest): Boolean =
        requests[request.requestId] === request && request.lifecycle.acceptsPresentationCallbacks

    private fun finishRequest(
        request: RewardedAdRequest,
        result: RewardedAdResult,
    ) {
        if (requests[request.requestId] !== request) return
        when (request.lifecycle.finish()) {
            RewardedAdFinishAction.COMPLETE -> request.result.complete(result)
            RewardedAdFinishAction.COMPLETE_AND_REMOVE -> {
                request.result.complete(result)
                requests.remove(request.requestId, request)
            }
            RewardedAdFinishAction.IGNORE -> Unit
        }
    }

    private companion object {
        const val DISMISS_CALLBACK_GRACE_PERIOD_MILLIS = 1_000L
        const val ACTIVITY_REATTACH_GRACE_PERIOD_MILLIS = 2_000L
    }

    private class RewardedAdRequest(
        val requestId: Long,
        val purpose: RewardedAdPurpose,
    ) {
        val result = CompletableDeferred<RewardedAdResult>()
        val lifecycle = RewardedAdRequestLifecycle()
    }
}

internal class RewardedAdRequestLifecycle {
    private var stage = Stage.LOADING
    private var abandonedWhileShowing = false

    val acceptsPresentationCallbacks: Boolean
        get() = stage == Stage.LOADING || stage == Stage.PENDING_ACTIVITY

    fun tryMarkPendingActivity(): Boolean {
        if (stage != Stage.LOADING) return false
        stage = Stage.PENDING_ACTIVITY
        return true
    }

    fun tryMarkShowing(): Boolean {
        if (stage != Stage.LOADING && stage != Stage.PENDING_ACTIVITY) return false
        stage = Stage.SHOWING
        return true
    }

    fun consume(): RewardedAdConsumeAction =
        when (stage) {
            Stage.SHOWING -> {
                abandonedWhileShowing = true
                RewardedAdConsumeAction.ABANDON_SHOWING
            }
            Stage.CANCELLED -> RewardedAdConsumeAction.IGNORE
            Stage.LOADING,
            Stage.PENDING_ACTIVITY,
            Stage.FINISHED,
            -> {
                stage = Stage.CANCELLED
                RewardedAdConsumeAction.CANCEL
            }
        }

    fun finish(): RewardedAdFinishAction =
        when (stage) {
            Stage.LOADING,
            Stage.PENDING_ACTIVITY,
            Stage.SHOWING,
            -> {
                stage = Stage.FINISHED
                if (abandonedWhileShowing) {
                    RewardedAdFinishAction.COMPLETE_AND_REMOVE
                } else {
                    RewardedAdFinishAction.COMPLETE
                }
            }
            Stage.FINISHED,
            Stage.CANCELLED,
            -> RewardedAdFinishAction.IGNORE
        }

    private enum class Stage {
        LOADING,
        PENDING_ACTIVITY,
        SHOWING,
        FINISHED,
        CANCELLED,
    }
}

internal enum class RewardedAdConsumeAction {
    CANCEL,
    ABANDON_SHOWING,
    IGNORE,
}

internal enum class RewardedAdFinishAction {
    COMPLETE,
    COMPLETE_AND_REMOVE,
    IGNORE,
}

private val RewardedAdPurpose.adUnitResource: Int
    get() =
        when (this) {
            RewardedAdPurpose.BANDALART_CREATION -> R.string.admob_rewarded_bandalart_creation_ad_unit_id
            RewardedAdPurpose.CLOUD_BACKUP -> R.string.admob_rewarded_cloud_backup_ad_unit_id
        }

internal class AndroidRewardedAdRewardPolicy(
    private val recordReward: suspend (Long) -> Boolean,
) {
    suspend fun complete(
        requestId: Long,
        purpose: RewardedAdPurpose,
    ): Boolean =
        when (purpose) {
            RewardedAdPurpose.BANDALART_CREATION -> recordReward(requestId)
            RewardedAdPurpose.CLOUD_BACKUP -> true
        }
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
