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

package com.nexters.bandalart.notification

import com.nexters.bandalart.core.domain.notification.DeadlineReminderReconciler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DeadlineReminderLifecycleBridge internal constructor(
    private val scope: CoroutineScope,
) {
    constructor() : this(CoroutineScope(SupervisorJob() + Dispatchers.Default))

    private var reconciler: DeadlineReminderReconciler? = null
    private var hasPendingReconcile = false

    fun record() {
        val currentReconciler = reconciler
        if (currentReconciler == null) {
            hasPendingReconcile = true
            return
        }
        scope.launch {
            currentReconciler.reconcileAll()
        }
    }

    internal fun attach(reconciler: DeadlineReminderReconciler) {
        this.reconciler = reconciler
        if (hasPendingReconcile) {
            hasPendingReconcile = false
            record()
        }
    }
}
