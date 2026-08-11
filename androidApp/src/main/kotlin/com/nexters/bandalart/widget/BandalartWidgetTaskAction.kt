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

package com.nexters.bandalart.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateIf
import com.nexters.bandalart.BandalartApplication
import com.nexters.bandalart.di.metro.setAndroidWidgetTaskCompleted
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException

private val TaskBandalartIdKey = ActionParameters.Key<Long>("taskBandalartId")
private val TaskSubGoalIdKey = ActionParameters.Key<Long>("taskSubGoalId")
private val TaskIdKey = ActionParameters.Key<Long>("taskId")
private val TaskCompletedKey = ActionParameters.Key<Boolean>("taskCompleted")

internal data class BandalartWidgetTaskActionRequest(
    val bandalartId: Long,
    val subGoalId: Long,
    val taskId: Long,
    val completed: Boolean,
)

internal fun taskActionParameters(request: BandalartWidgetTaskActionRequest): ActionParameters =
    actionParametersOf(
        TaskBandalartIdKey to request.bandalartId,
        TaskSubGoalIdKey to request.subGoalId,
        TaskIdKey to request.taskId,
        TaskCompletedKey to request.completed,
    )

internal fun ActionParameters.toTaskActionRequest(): BandalartWidgetTaskActionRequest? {
    val bandalartId = this[TaskBandalartIdKey]?.takeIf { it > 0L } ?: return null
    val subGoalId = this[TaskSubGoalIdKey]?.takeIf { it > 0L } ?: return null
    val taskId = this[TaskIdKey]?.takeIf { it > 0L } ?: return null
    val completed = this[TaskCompletedKey] ?: return null
    return BandalartWidgetTaskActionRequest(
        bandalartId = bandalartId,
        subGoalId = subGoalId,
        taskId = taskId,
        completed = completed,
    )
}

class BandalartWidgetTaskActionCallback : ActionCallback {
    @Suppress("TooGenericExceptionCaught")
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val request = parameters.toTaskActionRequest() ?: return
        val application = context.applicationContext as? BandalartApplication ?: return
        try {
            setAndroidWidgetTaskCompleted(
                appGraph = application.appGraph,
                bandalartId = request.bandalartId,
                subGoalId = request.subGoalId,
                taskId = request.taskId,
                completed = request.completed,
            )
            BandalartGlanceWidget().updateIf<Preferences>(context) { preferences ->
                preferences[BandalartIdKey] == request.bandalartId
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Napier.e("Failed to update a task from the widget", exception, tag = "BandalartWidget")
        }
    }
}
