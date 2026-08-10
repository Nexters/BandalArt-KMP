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

import com.nexters.bandalart.di.metro.AppGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

interface IosWidgetTimelineReloader {
    fun reloadTimelines()
}

class IosWidgetRuntimeBridge(
    private val timelineReloader: IosWidgetTimelineReloader,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var appGraph: AppGraph? = null

    internal fun attach(appGraph: AppGraph) {
        if (this.appGraph != null) return
        this.appGraph = appGraph
        scope.launch {
            appGraph.bandalartRepository.getBandalartList().collect {
                timelineReloader.reloadTimelines()
            }
        }
    }

    fun applicationDidBecomeActive() {
        val graph = appGraph ?: return
        graph.database.invalidationTracker.refreshAsync()
        scope.launch {
            graph.deadlineReminderReconciler.reconcileAll()
            timelineReloader.reloadTimelines()
        }
    }
}
