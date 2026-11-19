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
import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.nexters.bandalart.BuildConfig
import com.nexters.bandalart.core.common.AdPrivacyOptionsHost
import io.github.aakira.napier.Napier
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UMP 동의를 받은 뒤에만 광고 SDK를 초기화한다.
 *
 * 동의 대상 지역(EEA·영국·스위스 등)이 아니면 양식 없이 바로 광고를 요청할 수 있다.
 */
class AdConsentManager(
    context: Context,
    private val adsInitializer: AdsInitializer,
) : AdPrivacyOptionsHost {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)
    private val privacyOptionsRequired = MutableStateFlow(false)
    private var activity = WeakReference<Activity>(null)
    private val isConsentRequested = AtomicBoolean(false)

    override val isRequired: StateFlow<Boolean> = privacyOptionsRequired.asStateFlow()

    /**
     * 첫 화면의 onCreate에서 호출한다.
     * 진행 중이거나 이미 광고를 요청할 수 있으면 다시 확인하지 않는다. 확인이 실패해 광고를 요청할 수 없으면
     * 다음 onCreate에서 다시 시도한다.
     * 프로세스가 복원된 경우에도 광고 초기화가 필요하므로 savedInstanceState로 판단하지 않는다.
     */
    fun onActivityCreated(activity: Activity) {
        this.activity = WeakReference(activity)
        if (!isConsentRequested.compareAndSet(false, true)) return
        gatherConsent(activity)
    }

    override fun show() {
        val currentActivity = activity.get() ?: return
        UserMessagingPlatform.showPrivacyOptionsForm(currentActivity) { formError ->
            logFormError("Privacy options form failed", formError)
            onConsentGatheringFinished()
        }
    }

    private fun gatherConsent(activity: Activity) {
        consentInformation.requestConsentInfoUpdate(
            activity,
            consentRequestParameters(activity),
            {
                // 응답을 기다리는 동안 Activity가 재생성될 수 있으므로 지금 살아 있는 Activity로 양식을 띄운다.
                val currentActivity =
                    this.activity.get()?.takeUnless { it.isFinishing || it.isDestroyed }
                if (currentActivity == null) {
                    onConsentGatheringFinished()
                    return@requestConsentInfoUpdate
                }
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(currentActivity) { formError ->
                    logFormError("Consent form failed", formError)
                    onConsentGatheringFinished()
                }
            },
            { requestError ->
                logFormError("Consent info update failed", requestError)
                onConsentGatheringFinished()
            },
        )
        // 이전 실행에서 받은 동의가 있으면 갱신 응답을 기다리지 않고 광고를 준비한다.
        if (consentInformation.canRequestAds()) adsInitializer.initialize()
    }

    private fun onConsentGatheringFinished() {
        privacyOptionsRequired.value =
            consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (consentInformation.canRequestAds()) {
            adsInitializer.initialize()
        } else {
            adsInitializer.markUnavailable()
            isConsentRequested.set(false)
        }
    }

    private fun consentRequestParameters(context: Context): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        if (BuildConfig.UMP_DEBUG_GEOGRAPHY_EEA) {
            // 에뮬레이터는 UMP 테스트 기기로 자동 처리되므로 기기 ID 없이 지역만 바꾼다.
            builder.setConsentDebugSettings(
                ConsentDebugSettings
                    .Builder(context)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .build(),
            )
        }
        return builder.build()
    }

    private fun logFormError(
        message: String,
        formError: FormError?,
    ) {
        if (formError == null) return
        Napier.w("$message: ${formError.errorCode} ${formError.message}", tag = TAG)
    }

    private companion object {
        const val TAG = "AdConsent"
    }
}
