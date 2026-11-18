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

import android.content.Context
import android.content.pm.PackageManager
import com.nexters.bandalart.core.domain.widget.HomeWidgetPlatform
import com.nexters.bandalart.core.domain.widget.HomeWidgetSupport

/** 위젯을 지원하지 않는 런처·기기에서는 위젯 안내를 띄우지 않는다. */
class AndroidHomeWidgetSupport(
    private val context: Context,
) : HomeWidgetSupport {
    override fun supportedPlatform(): HomeWidgetPlatform? =
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_APP_WIDGETS)) {
            HomeWidgetPlatform.ANDROID
        } else {
            null
        }
}
