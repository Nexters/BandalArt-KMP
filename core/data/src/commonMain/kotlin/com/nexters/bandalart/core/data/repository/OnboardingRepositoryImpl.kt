/*
 * Copyright 2025 easyhooon
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
import com.nexters.bandalart.core.domain.repository.OnboardingRepository

class OnboardingRepositoryImpl(
    private val bandalartDataStore: BandalartDataStore,
) : OnboardingRepository {
    override suspend fun setOnboardingCompletedStatus(flag: Boolean) {
        bandalartDataStore.setOnboardingCompletedStatus(flag)
    }

    override suspend fun getOnboardingCompletedStatus(): Boolean = bandalartDataStore.getOnboardingCompletedStatus()
}
