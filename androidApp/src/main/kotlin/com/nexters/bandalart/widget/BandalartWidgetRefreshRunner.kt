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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class BandalartWidgetRefreshRunner(
    private val refresh: suspend () -> Unit,
) {
    private val mutex = Mutex()

    suspend fun refresh() {
        mutex.withLock {
            refresh.invoke()
        }
    }
}
