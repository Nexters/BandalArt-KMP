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

data class IosWidgetBandalartOption(
    val id: Long,
    val title: String,
    val profileEmoji: String?,
)

data class IosWidgetSubGoalOption(
    val id: Long,
    val bandalartId: Long,
    val title: String,
)

data class IosWidgetSnapshot(
    val bandalartId: Long,
    val subGoalId: Long?,
    val title: String,
    val profileEmoji: String?,
    val completionRatio: Int,
    val subGoalTitle: String?,
    val tasks: List<IosWidgetTask>,
)

data class IosWidgetTask(
    val id: Long,
    val title: String,
    val isCompleted: Boolean,
)
