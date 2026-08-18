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

package com.nexters.bandalart.core.domain.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupSnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val bandalarts: List<BackupBandalart>,
    val cells: List<BackupCell>,
    val preferences: BackupPreferences,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@Serializable
data class BackupBandalart(
    val id: Long,
    val mainColor: String,
    val subColor: String,
    val profileEmoji: String? = null,
    val title: String? = null,
    val description: String? = null,
    val dueDate: String? = null,
    val isCompleted: Boolean = false,
    val completionRatio: Int = 0,
)

@Serializable
data class BackupCell(
    val id: Long,
    val bandalartId: Long,
    val title: String? = null,
    val description: String? = null,
    val dueDate: String? = null,
    val isCompleted: Boolean = false,
    val parentId: Long? = null,
)

@Serializable
data class BackupPreferences(
    val recentBandalartId: Long,
    val recentSubGoalIds: Map<Long, Long>,
    val completedBandalarts: List<BackupCompletedBandalart>,
    val onboardingCompleted: Boolean,
    val themeMode: String?,
    val recentEmojis: List<String>,
    val deadlineReminderEnabled: Boolean,
    val maxBandalartSlots: Int,
)

@Serializable
data class BackupCompletedBandalart(
    val bandalartId: Long,
    val isCompleted: Boolean,
)

@Serializable
data class BackupMetadata(
    val bandalartCount: Int,
    val updatedAt: String,
)
