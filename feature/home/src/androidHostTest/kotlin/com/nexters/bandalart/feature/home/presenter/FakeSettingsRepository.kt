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

package com.nexters.bandalart.feature.home.presenter

import com.nexters.bandalart.core.domain.entity.ThemeMode
import com.nexters.bandalart.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSettingsRepository(
    initialThemeMode: ThemeMode = ThemeMode.SYSTEM,
    initialDeadlineReminderEnabled: Boolean = false,
    private val beforeRecentEmojiSave: suspend (String) -> Unit = {},
) : SettingsRepository {
    private val themeModeState = MutableStateFlow(initialThemeMode)
    private val recentEmojisState = MutableStateFlow<List<String>>(emptyList())
    private val deadlineReminderEnabledState = MutableStateFlow(initialDeadlineReminderEnabled)

    override val themeMode = themeModeState.asStateFlow()
    override val recentEmojis = recentEmojisState.asStateFlow()
    override val deadlineReminderEnabled = deadlineReminderEnabledState.asStateFlow()

    val savedThemeModes = mutableListOf<ThemeMode>()
    val savedRecentEmojis = mutableListOf<String>()

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        savedThemeModes += themeMode
        themeModeState.value = themeMode
    }

    override suspend fun addRecentEmoji(emoji: String) {
        beforeRecentEmojiSave(emoji)
        savedRecentEmojis += emoji
        recentEmojisState.value =
            (listOf(emoji) + recentEmojisState.value.filterNot { it == emoji })
                .take(12)
    }

    override suspend fun setDeadlineReminderEnabled(enabled: Boolean) {
        deadlineReminderEnabledState.value = enabled
    }
}
