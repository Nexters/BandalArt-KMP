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

package com.nexters.bandalart.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

expect class BandalartDataStoreFactory {
    fun createBandalartDataStore(): DataStore<Preferences>

    fun createInAppUpdateDataStore(): DataStore<Preferences>
}

internal const val BANDALART_DATA_STORE_FILE_NAME = "bandalart.preferences_pb"
internal const val IN_APP_UPDATE_DATA_STORE_FILE_NAME = "in_app_update.preferences_pb"
