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

const val SUPPORT_EMAIL_ADDRESS = "mraz3068@gmail.com"

data class SupportMailDraft(
    val recipient: String = SUPPORT_EMAIL_ADDRESS,
    val subject: String,
    val body: String,
)

enum class SupportMailOpenResult {
    OPENED,
    UNAVAILABLE,
    FAILED,
}

interface SupportMailLauncher {
    val platformName: String

    fun open(draft: SupportMailDraft): SupportMailOpenResult

    fun copyToClipboard(text: String): Boolean
}

fun SupportMailLauncher.openWithClipboardFallback(draft: SupportMailDraft): SupportMailOpenResult =
    open(draft).also { result ->
        if (result != SupportMailOpenResult.OPENED) {
            copyToClipboard(draft.recipient)
        }
    }

internal fun SupportMailDraft.toMailtoUri(): String =
    buildString {
        append("mailto:")
        append(recipient.percentEncode(allowMailAddressSymbols = true))
        append("?subject=")
        append(subject.percentEncode())
        append("&body=")
        append(body.percentEncode())
    }

private fun String.percentEncode(allowMailAddressSymbols: Boolean = false): String =
    buildString {
        this@percentEncode.encodeToByteArray().forEach { byte ->
            val value = byte.toInt() and 0xFF
            val character = value.toChar()
            val isAllowed =
                character.isAsciiLetterOrDigit() ||
                    character in "-._~" ||
                    (allowMailAddressSymbols && character in "@+")

            if (isAllowed) {
                append(character)
            } else {
                append('%')
                append(HEX_DIGITS[value shr 4])
                append(HEX_DIGITS[value and 0x0F])
            }
        }
    }

private fun Char.isAsciiLetterOrDigit(): Boolean = this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9'

private const val HEX_DIGITS = "0123456789ABCDEF"
