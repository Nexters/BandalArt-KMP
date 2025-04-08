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

import com.nexters.bandalart.core.datastore.InAppUpdateDataStore
import com.nexters.bandalart.core.domain.repository.InAppUpdateRepository

class DefaultInAppUpdateRepository(
    private val inAppUpdateDataStore: InAppUpdateDataStore,
) : InAppUpdateRepository {
    override suspend fun setLastRejectedUpdateVersion(rejectedVersionCode: Int) {
        inAppUpdateDataStore.setLastRejectedUpdateVersion(rejectedVersionCode)
    }

    override suspend fun isUpdateAlreadyRejected(updateVersionCode: Int): Boolean {
        val lastRejectedUpdateVersion = inAppUpdateDataStore.getLastRejectedUpdateVersion()
        return updateVersionCode <= lastRejectedUpdateVersion
    }
}
