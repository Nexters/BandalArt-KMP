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
) : SettingsRepository {
    private val themeModeState = MutableStateFlow(initialThemeMode)

    override val themeMode = themeModeState.asStateFlow()

    val savedThemeModes = mutableListOf<ThemeMode>()

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        savedThemeModes += themeMode
        themeModeState.value = themeMode
    }
}
