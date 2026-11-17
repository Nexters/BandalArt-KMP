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

package com.nexters.bandalart.core.domain.widget

/** 홈 화면 위젯을 추가할 수 있는 플랫폼. 위젯 추가 방법 안내 문구를 고를 때도 쓴다. */
enum class HomeWidgetPlatform {
    ANDROID,
    IOS,
}

/** 현재 기기에서 반다라트 홈 화면 위젯을 추가할 수 있는지 알려 준다. */
fun interface HomeWidgetSupport {
    /** 위젯을 추가할 수 있으면 해당 플랫폼, 지원하지 않는 기기면 null. */
    fun supportedPlatform(): HomeWidgetPlatform?
}

object UnsupportedHomeWidgetSupport : HomeWidgetSupport {
    override fun supportedPlatform(): HomeWidgetPlatform? = null
}
