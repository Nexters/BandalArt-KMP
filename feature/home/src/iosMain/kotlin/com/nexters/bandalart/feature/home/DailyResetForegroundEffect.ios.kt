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
import androidx.compose.runtime.rememberUpdatedState
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationWillResignActiveNotification

@Composable
internal actual fun DailyResetForegroundEffect(
    onForeground: () -> Unit,
    onBackground: () -> Unit,
) {
    val currentOnForeground = rememberUpdatedState(onForeground)
    val currentOnBackground = rememberUpdatedState(onBackground)
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val foregroundObserver =
            center.addObserverForName(
                name = UIApplicationDidBecomeActiveNotification,
                `object` = null,
                queue = NSOperationQueue.mainQueue,
            ) {
                currentOnForeground.value()
            }
        val backgroundObserver =
            center.addObserverForName(
                name = UIApplicationWillResignActiveNotification,
                `object` = null,
                queue = NSOperationQueue.mainQueue,
            ) {
                currentOnBackground.value()
            }
        onDispose {
            center.removeObserver(foregroundObserver)
            center.removeObserver(backgroundObserver)
        }
    }
}
