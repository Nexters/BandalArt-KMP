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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BandalartSlotRepositoryTest {
    private val dataStore = mockk<BandalartDataStore>()
    private val repository = DefaultBandalartSlotRepository(dataStore)

    @Test
    fun currentCountIsUsedAsTheMinimumForExistingUsers() =
        runTest {
            coEvery { dataStore.resolveMaxBandalartSlots(5) } returns 5

            assertEquals(5, repository.getMaxBandalartSlots(currentBandalartCount = 5))

            coVerify(exactly = 1) { dataStore.resolveMaxBandalartSlots(5) }
        }

    @Test
    fun expansionUsesTheFreeSlotCountAsTheMinimumForNewUsers() =
        runTest {
            coEvery { dataStore.expandMaxBandalartSlots(3) } returns 4

            assertEquals(4, repository.expandMaxBandalartSlots(currentBandalartCount = 2))

            coVerify(exactly = 1) { dataStore.expandMaxBandalartSlots(3) }
        }
}
