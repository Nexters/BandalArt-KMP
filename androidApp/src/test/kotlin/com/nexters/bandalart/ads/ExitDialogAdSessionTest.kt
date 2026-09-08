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

package com.nexters.bandalart.ads

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExitDialogAdSessionTest {
    @Test
    fun adLoadedWhileDialogIsOpenIsDeferredUntilNextDialog() {
        val session = ExitDialogAdSession<String>()
        var availableAd: String? = null

        session.open(availableAd)
        availableAd = "next ad"
        assertNull(session.ad)

        session.closeAndWasAdDisplayed()
        session.open(availableAd)
        assertEquals("next ad", session.ad)
    }

    @Test
    fun onlyDisplayedAdIsRecycledWhenDialogCloses() {
        val session = ExitDialogAdSession<String>()

        session.open(availableAd = null)
        assertFalse(session.closeAndWasAdDisplayed())

        session.open(availableAd = "displayed ad")
        assertTrue(session.closeAndWasAdDisplayed())
        assertNull(session.ad)
    }
}
