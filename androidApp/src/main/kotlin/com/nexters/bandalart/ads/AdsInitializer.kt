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

import android.content.Context
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader
import com.nexters.bandalart.R
import io.github.aakira.napier.Napier
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AdsInitializer(
    private val context: Context,
) {
    private val isInitializationStarted = AtomicBoolean(false)
    private val initialization = CompletableDeferred<Boolean>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize() {
        if (!isInitializationStarted.compareAndSet(false, true)) return

        scope.launch {
            runCatching {
                MobileAds.initialize(
                    context,
                    InitializationConfig.Builder(context.getString(R.string.admob_app_id)).build(),
                )
                MobileAds.putPublisherFirstPartyIdEnabled(false)

                distinctRewardedAdUnitIds(
                    context.getString(R.string.admob_rewarded_bandalart_creation_ad_unit_id),
                    context.getString(R.string.admob_rewarded_cloud_backup_ad_unit_id),
                ).forEach { adUnitId ->
                    runCatching {
                        RewardedAdPreloader.start(
                            adUnitId,
                            PreloadConfiguration(
                                AdRequest
                                    .Builder(adUnitId)
                                    .setGoogleExtrasBundle(nonPersonalizedAdExtras())
                                    .build(),
                            ),
                        )
                    }.onFailure { exception ->
                        Log.e("AdsInitializer", "Rewarded ad preloader failed to start", exception)
                        Napier.e("Rewarded ad preloader failed to start", exception, tag = "AdsInitializer")
                    }
                }

                initialization.complete(true)
                Napier.d("GMA Next-Gen SDK initialized", tag = "AdsInitializer")
            }.onFailure { exception ->
                initialization.complete(false)
                Log.e("AdsInitializer", "GMA Next-Gen SDK initialization failed", exception)
                Napier.e("GMA Next-Gen SDK initialization failed", exception, tag = "AdsInitializer")
            }
        }
    }

    suspend fun awaitInitialized(): Boolean {
        initialize()
        return initialization.await()
    }
}

internal fun distinctRewardedAdUnitIds(vararg adUnitIds: String): Set<String> = adUnitIds.toSet()
