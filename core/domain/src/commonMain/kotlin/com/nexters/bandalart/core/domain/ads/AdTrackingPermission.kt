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

package com.nexters.bandalart.core.domain.ads

/**
 * 홈에 처음 들어왔을 때 광고 추적 권한(iOS ATT)과 광고 동의 확인을 시작한다.
 *
 * ATT를 아직 묻지 않았으면 홈이 반다라트 설명 다이얼로그를 먼저 보여 준 뒤 [request]를 호출하고,
 * 그다음 [startAdConsent]로 광고 동의 확인을 시작한다.
 */
interface AdTrackingPermission {
    /** ATT 권한을 아직 묻지 않아 설명 다이얼로그를 먼저 보여야 하면 true. */
    fun isRequestNeeded(): Boolean

    /** ATT 시스템 팝업을 띄우고 사용자가 답할 때까지 기다린다. */
    suspend fun request()

    /** 광고 동의 확인을 시작한다. ATT를 물을 필요가 없거나 답을 받은 뒤 호출한다. */
    fun startAdConsent()
}

/** ATT가 없는 플랫폼용. Android는 앱 시작 시 광고 동의를 따로 확인한다. */
object NoOpAdTrackingPermission : AdTrackingPermission {
    override fun isRequestNeeded(): Boolean = false

    override suspend fun request() = Unit

    override fun startAdConsent() = Unit
}
