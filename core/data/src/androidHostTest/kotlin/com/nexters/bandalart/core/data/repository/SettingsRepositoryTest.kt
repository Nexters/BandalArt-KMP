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

package com.nexters.bandalart.core.data.repository

import com.nexters.bandalart.core.datastore.BandalartDataStore
import com.nexters.bandalart.core.domain.entity.ThemeMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SettingsRepositoryTest {
    private val dataStore = mockk<BandalartDataStore>()

    @Test
    fun storedValuesAreMappedToThemeModeWithSystemFallback() =
        runTest {
            every { dataStore.themeMode } returns flowOf("unexpected")

            val repository = DefaultSettingsRepository(dataStore)

            assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
        }

    @Test
    fun themeSelectionIsStoredAsStableString() =
        runTest {
            every { dataStore.themeMode } returns flowOf(null)
            coEvery { dataStore.setThemeMode(any()) } returns Unit
            val repository = DefaultSettingsRepository(dataStore)

            repository.setThemeMode(ThemeMode.DARK)

            coVerify(exactly = 1) { dataStore.setThemeMode("dark") }
        }
}
