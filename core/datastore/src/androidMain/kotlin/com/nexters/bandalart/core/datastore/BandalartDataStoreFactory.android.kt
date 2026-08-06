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

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

actual class BandalartDataStoreFactory(
    private val context: Context
) {
    actual fun createBandalartDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                context.filesDir
                    .resolve(ANDROID_BANDALART_DATA_STORE_RELATIVE_PATH)
                    .absolutePath
                    .toPath()
            }
        )

    actual fun createInAppUpdateDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                context.filesDir
                    .resolve(ANDROID_IN_APP_UPDATE_DATA_STORE_RELATIVE_PATH)
                    .absolutePath
                    .toPath()
            }
        )
}

internal const val ANDROID_BANDALART_DATA_STORE_RELATIVE_PATH = "datastore/bandalart_datastore.preferences_pb"
internal const val ANDROID_IN_APP_UPDATE_DATA_STORE_RELATIVE_PATH = "datastore/in_app_update_datastore.preferences_pb"
