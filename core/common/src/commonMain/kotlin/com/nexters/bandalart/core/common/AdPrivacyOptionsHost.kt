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

package com.nexters.bandalart.core.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 광고 동의를 다시 선택할 수 있는 개인정보 옵션 진입점.
 *
 * [isRequired]가 true인 동의 대상 지역 사용자에게만 설정 화면에 진입점을 노출한다.
 */
interface AdPrivacyOptionsHost {
    val isRequired: StateFlow<Boolean>

    fun show()
}

object NoOpAdPrivacyOptionsHost : AdPrivacyOptionsHost {
    override val isRequired: StateFlow<Boolean> = MutableStateFlow(false)

    override fun show() = Unit
}
