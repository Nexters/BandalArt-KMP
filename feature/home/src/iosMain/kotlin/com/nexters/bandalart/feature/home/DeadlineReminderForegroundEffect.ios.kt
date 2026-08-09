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

package com.nexters.bandalart.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSystemTimeZoneDidChangeNotification
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationSignificantTimeChangeNotification

@Composable
internal actual fun DeadlineReminderForegroundEffect(onForeground: () -> Unit) {
    val currentOnForeground = rememberUpdatedState(onForeground)
    LaunchedEffect(Unit) {
        currentOnForeground.value()
    }
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val names =
            listOf(
                UIApplicationDidBecomeActiveNotification,
                UIApplicationSignificantTimeChangeNotification,
                NSSystemTimeZoneDidChangeNotification,
            )
        val observers =
            names.map { name ->
                center.addObserverForName(
                    name = name,
                    `object` = null,
                    queue = NSOperationQueue.mainQueue,
                ) {
                    currentOnForeground.value()
                }
            }
        onDispose {
            observers.forEach(center::removeObserver)
        }
    }
}
