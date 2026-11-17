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

package com.nexters.bandalart.widget

import com.nexters.bandalart.core.domain.widget.HomeWidgetPlatform
import com.nexters.bandalart.core.domain.widget.HomeWidgetSupport
import platform.Foundation.NSProcessInfo
import platform.Foundation.iOSAppOnMac
import platform.UIKit.UIDevice

// 위젯 extension의 최소 배포 버전. 앱은 iOS 16.6부터 설치되지만 위젯은 17.0부터 쓸 수 있다.
private const val WIDGET_MINIMUM_IOS_MAJOR_VERSION = 17

/** iOS 17 미만이나 Mac에서 실행 중인 iOS 앱에서는 홈 화면 위젯 안내를 띄우지 않는다. */
class IosHomeWidgetSupport : HomeWidgetSupport {
    override fun supportedPlatform(): HomeWidgetPlatform? {
        val processInfo = NSProcessInfo.processInfo
        if (processInfo.iOSAppOnMac) return null
        val majorVersion =
            UIDevice.currentDevice.systemVersion
                .substringBefore('.')
                .toIntOrNull() ?: return null
        return if (majorVersion >= WIDGET_MINIMUM_IOS_MAJOR_VERSION) HomeWidgetPlatform.IOS else null
    }
}
