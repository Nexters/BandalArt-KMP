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

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.nexters.bandalart.core.domain.entity.BandalartWidgetSnapshot
import com.nexters.bandalart.core.domain.entity.BandalartWidgetTask

internal val BandalartIdKey = longPreferencesKey("bandalartId")
internal val SubGoalIdKey = longPreferencesKey("subGoalId")

internal data class BandalartWidgetSelection(
    val bandalartId: Long,
    val subGoalId: Long?,
)

internal fun Preferences.toWidgetSelection(): BandalartWidgetSelection? {
    val bandalartId = this[BandalartIdKey] ?: return null
    return bandalartId
        .takeIf { it > 0L }
        ?.let { BandalartWidgetSelection(bandalartId = it, subGoalId = this[SubGoalIdKey]) }
}

internal fun MutablePreferences.setWidgetSelection(
    bandalartId: Long,
    subGoalId: Long?,
) {
    this[BandalartIdKey] = bandalartId
    subGoalId?.let { this[SubGoalIdKey] = it } ?: remove(SubGoalIdKey)
}

internal suspend fun saveWidgetConfiguration(
    selection: BandalartWidgetSelection,
    setRecentBandalartId: suspend (Long) -> Unit,
    setRecentSubGoalId: suspend (Long, Long) -> Unit,
    persistSelection: suspend (BandalartWidgetSelection) -> Unit,
) {
    persistSelection(selection)
    selection.subGoalId?.let { setRecentSubGoalId(selection.bandalartId, it) }
    setRecentBandalartId(selection.bandalartId)
}

internal fun subGoalIdAfterBandalartSelection(
    currentBandalartId: Long?,
    currentSubGoalId: Long?,
    selectedBandalartId: Long,
): Long? = currentSubGoalId.takeIf { currentBandalartId == selectedBandalartId }

internal fun resolveWidgetSelection(
    configuredSelection: BandalartWidgetSelection?,
    recentBandalartId: Long,
    recentSubGoalId: Long,
    availableSubGoalIds: List<Long>,
): BandalartWidgetSelection? {
    val bandalartId = recentBandalartId.takeIf { it > 0L } ?: configuredSelection?.bandalartId ?: return null
    val configuredSubGoalId =
        configuredSelection
            ?.takeIf { it.bandalartId == bandalartId }
            ?.subGoalId
            ?.takeIf(availableSubGoalIds::contains)
    val subGoalId =
        recentSubGoalId
            .takeIf { it > 0L && availableSubGoalIds.contains(it) }
            ?: configuredSubGoalId
            ?: availableSubGoalIds.firstOrNull()
    return BandalartWidgetSelection(
        bandalartId = bandalartId,
        subGoalId = subGoalId,
    )
}

internal fun resolveWidgetBandalartId(
    configuredSelection: BandalartWidgetSelection?,
    recentBandalartId: Long,
): Long? = recentBandalartId.takeIf { it > 0L } ?: configuredSelection?.bandalartId

internal enum class BandalartWidgetLayout(
    val taskLimit: Int,
) {
    SMALL(taskLimit = 0),
    MEDIUM(taskLimit = 2),
    LARGE(taskLimit = 5),
}

internal fun resolveWidgetLayout(
    widthDp: Int,
    heightDp: Int,
): BandalartWidgetLayout =
    when {
        heightDp >= 200 -> BandalartWidgetLayout.LARGE
        widthDp >= 220 -> BandalartWidgetLayout.MEDIUM
        else -> BandalartWidgetLayout.SMALL
    }

internal sealed interface BandalartWidgetViewState {
    data object Unconfigured : BandalartWidgetViewState

    data object Deleted : BandalartWidgetViewState

    data class Content(
        val bandalartId: Long,
        val subGoalId: Long?,
        val title: String,
        val profileEmoji: String,
        val completionRatio: Int,
        val subGoalTitle: String?,
        val tasks: List<BandalartWidgetTask>,
    ) : BandalartWidgetViewState
}

internal fun toWidgetViewState(
    selection: BandalartWidgetSelection?,
    snapshot: BandalartWidgetSnapshot?,
    unnamedGoalTitle: String,
): BandalartWidgetViewState =
    when {
        selection == null -> BandalartWidgetViewState.Unconfigured
        snapshot == null -> BandalartWidgetViewState.Deleted
        else ->
            BandalartWidgetViewState.Content(
                bandalartId = snapshot.bandalartId,
                subGoalId = snapshot.subGoalId,
                title = snapshot.title.ifBlank { unnamedGoalTitle },
                profileEmoji = snapshot.profileEmoji?.ifBlank { null } ?: "🎯",
                completionRatio = snapshot.completionRatio.coerceIn(0, 100),
                subGoalTitle = snapshot.subGoalTitle?.ifBlank { null },
                tasks = snapshot.tasks.filter { it.title.isNotBlank() }.take(5),
            )
    }

internal fun BandalartWidgetViewState.Content.tasksFor(layout: BandalartWidgetLayout): List<BandalartWidgetTask> = tasks.take(layout.taskLimit)
