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

package com.nexters.bandalart.core.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SupportMailLauncherTest {
    @Test
    fun mailtoUriEncodesRecipientSubjectAndBody() {
        val uri =
            SupportMailDraft(
                recipient = "bandalart+support@example.com",
                subject = "[반다라트 문의]",
                body = "문의 내용을 작성해 주세요.\n앱 버전: 2.2.9 & Android",
            ).toMailtoUri()

        assertTrue(uri.startsWith("mailto:bandalart+support@example.com?subject=%5B"))
        assertTrue(uri.contains("&body="))
        assertTrue(uri.contains("%0A"))
        assertTrue(uri.contains("%20"))
        assertTrue(uri.contains("%26"))
        assertFalse(uri.contains(" "))
        assertFalse(uri.contains("\n"))
        assertFalse(uri.contains("문의"))
    }

    @Test
    fun openedMailDoesNotCopyRecipient() {
        val launcher = FakeSupportMailLauncher(SupportMailOpenResult.OPENED)
        val draft = SupportMailDraft(subject = "subject", body = "body")

        assertEquals(SupportMailOpenResult.OPENED, launcher.openWithClipboardFallback(draft))
        assertNull(launcher.copiedText)
    }

    @Test
    fun unavailableOrFailedMailCopiesRecipient() {
        listOf(
            SupportMailOpenResult.UNAVAILABLE,
            SupportMailOpenResult.FAILED,
        ).forEach { openResult ->
            val launcher = FakeSupportMailLauncher(openResult)
            val draft = SupportMailDraft(subject = "subject", body = "body")

            assertEquals(openResult, launcher.openWithClipboardFallback(draft))
            assertEquals(SUPPORT_EMAIL_ADDRESS, launcher.copiedText)
        }
    }
}

private class FakeSupportMailLauncher(
    private val openResult: SupportMailOpenResult,
) : SupportMailLauncher {
    override val platformName: String = "Test"
    var copiedText: String? = null

    override fun open(draft: SupportMailDraft): SupportMailOpenResult = openResult

    override fun copyToClipboard(text: String): Boolean {
        copiedText = text
        return true
    }
}
