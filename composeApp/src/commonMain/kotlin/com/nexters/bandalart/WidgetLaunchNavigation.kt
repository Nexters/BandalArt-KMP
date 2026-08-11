/*
 * Copyright 2026 easyhooon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nexters.bandalart

import com.nexters.bandalart.feature.complete.CompleteScreen
import com.nexters.bandalart.feature.home.HomeScreen
import com.slack.circuit.runtime.screen.Screen

internal fun widgetLaunchDestination(
    currentScreen: Screen,
    pendingBandalartId: Long?,
): Screen? =
    HomeScreen.takeIf {
        pendingBandalartId != null && currentScreen is CompleteScreen
    }
