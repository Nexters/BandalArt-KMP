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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface BandalartWidgetLaunchTarget {
    val pendingBandalartId: StateFlow<Long?>

    fun record(bandalartId: Long)

    fun acknowledge(bandalartId: Long)
}

class BufferedBandalartWidgetLaunchTarget : BandalartWidgetLaunchTarget {
    private val mutablePendingBandalartId = MutableStateFlow<Long?>(null)

    override val pendingBandalartId: StateFlow<Long?> = mutablePendingBandalartId.asStateFlow()

    override fun record(bandalartId: Long) {
        if (bandalartId > 0L) mutablePendingBandalartId.value = bandalartId
    }

    override fun acknowledge(bandalartId: Long) {
        mutablePendingBandalartId.compareAndSet(expect = bandalartId, update = null)
    }
}
