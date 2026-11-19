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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AdsInitializer(
    private val context: Context,
) {
    private val isInitializationStarted = AtomicBoolean(false)

    /** null: 동의 대기, true: 초기화 완료, false: 광고 불가 또는 초기화 실패 */
    private val initialization = MutableStateFlow<Boolean?>(null)
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
                                    .build(),
                            ),
                        )
                    }.onFailure { exception ->
                        Log.e("AdsInitializer", "Rewarded ad preloader failed to start", exception)
                        Napier.e("Rewarded ad preloader failed to start", exception, tag = "AdsInitializer")
                    }
                }

                initialization.value = true
                Napier.d("GMA Next-Gen SDK initialized", tag = "AdsInitializer")
            }.onFailure { exception ->
                initialization.value = false
                Log.e("AdsInitializer", "GMA Next-Gen SDK initialization failed", exception)
                Napier.e("GMA Next-Gen SDK initialization failed", exception, tag = "AdsInitializer")
            }
        }
    }

    /**
     * 동의 흐름이 끝났는데 광고를 요청할 수 없을 때 호출한다.
     * 대기 중인 광고 지면은 false를 받는다. 이후 동의를 받으면 [initialize]로 다시 초기화할 수 있다.
     */
    fun markUnavailable() {
        if (isInitializationStarted.get()) return
        initialization.value = false
    }

    /** 초기화는 [initialize] 또는 [markUnavailable]이 결정한다. 광고 동의 전에는 초기화를 시작하지 않는다. */
    suspend fun awaitInitialized(): Boolean = initialization.filterNotNull().first()
}

internal fun distinctRewardedAdUnitIds(vararg adUnitIds: String): Set<String> = adUnitIds.toSet()
