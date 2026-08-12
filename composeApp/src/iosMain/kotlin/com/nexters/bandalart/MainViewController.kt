/*
 * Copyright 2025 easyhooon
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

package com.nexters.bandalart

import androidx.compose.ui.window.ComposeUIViewController
import com.nexters.bandalart.ads.IosAdsBridge
import com.nexters.bandalart.di.metro.createIosAppGraph
import com.nexters.bandalart.notification.DeadlineNotificationLaunchBridge
import com.nexters.bandalart.notification.DeadlineReminderLifecycleBridge
import com.nexters.bandalart.widget.IosWidgetLaunchBridge
import com.nexters.bandalart.widget.IosWidgetRuntimeBridge
import platform.UIKit.UIViewController

@Suppress("FunctionName")
fun MainViewController(
    notificationLaunchBridge: DeadlineNotificationLaunchBridge,
    deadlineReminderLifecycleBridge: DeadlineReminderLifecycleBridge,
    adsBridge: IosAdsBridge,
    widgetLaunchBridge: IosWidgetLaunchBridge,
    widgetRuntimeBridge: IosWidgetRuntimeBridge,
): UIViewController {
    val appGraph = createIosAppGraph(adsBridge)
    notificationLaunchBridge.attach(appGraph.deadlineNotificationLaunchTarget)
    deadlineReminderLifecycleBridge.attach(appGraph.deadlineReminderReconciler)
    widgetLaunchBridge.attach(appGraph.bandalartWidgetLaunchTarget)
    widgetRuntimeBridge.attach(appGraph)

    return ComposeUIViewController {
        BandalartApp(appGraph = appGraph)
    }
}
