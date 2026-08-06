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

package com.nexters.bandalart.core.datastore

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Android DataStore 파일 경로 호환성")
class BandalartDataStoreFactoryAndroidTest {
    @Test
    fun bandalartDataStoreUsesLegacyRelativePath() {
        assertEquals(
            "datastore/bandalart_datastore.preferences_pb",
            ANDROID_BANDALART_DATA_STORE_RELATIVE_PATH,
        )
    }

    @Test
    fun inAppUpdateDataStoreUsesLegacyRelativePath() {
        assertEquals(
            "datastore/in_app_update_datastore.preferences_pb",
            ANDROID_IN_APP_UPDATE_DATA_STORE_RELATIVE_PATH,
        )
    }
}
