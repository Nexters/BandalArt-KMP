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

package com.nexters.bandalart.backup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class AndroidDeviceBackupKeyProviderTest {
    @Test
    fun ssaidIsNamespacedAndHashedAsLowercaseSha256() {
        assertEquals(
            "73004b6c7a0e5051b3eb499961d183f4964195a27f235e0672a5a13e117aa9ae",
            deriveDeviceBackupKey("com.nexters.bandalart", "android-id"),
        )
    }

    @Test
    fun appNamespaceChangesTheDerivedKey() {
        assertNotEquals(
            deriveDeviceBackupKey("com.nexters.bandalart", "android-id"),
            deriveDeviceBackupKey("another.app", "android-id"),
        )
    }
}
