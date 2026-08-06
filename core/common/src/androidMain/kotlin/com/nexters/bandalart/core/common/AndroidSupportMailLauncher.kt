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

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.aakira.napier.Napier

class AndroidSupportMailLauncher(
    private val application: Application,
) : SupportMailLauncher {
    override val platformName: String = "Android"

    @Suppress("TooGenericExceptionCaught")
    override fun open(draft: SupportMailDraft): SupportMailOpenResult {
        val intent =
            Intent(Intent.ACTION_SENDTO, Uri.parse(draft.toMailtoUri()))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (intent.resolveActivity(application.packageManager) == null) {
            return SupportMailOpenResult.UNAVAILABLE
        }

        return try {
            application.startActivity(intent)
            SupportMailOpenResult.OPENED
        } catch (exception: Exception) {
            Napier.e("Failed to open support mail app", exception, tag = "SupportMail")
            SupportMailOpenResult.FAILED
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun copyToClipboard(text: String): Boolean =
        try {
            val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("BandalArt support email", text))
            true
        } catch (exception: Exception) {
            Napier.e("Failed to copy support email", exception, tag = "SupportMail")
            false
        }
}
