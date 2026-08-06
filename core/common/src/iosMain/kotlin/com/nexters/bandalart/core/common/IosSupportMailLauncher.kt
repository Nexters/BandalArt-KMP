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

import io.github.aakira.napier.Napier
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard

class IosSupportMailLauncher : SupportMailLauncher {
    override val platformName: String = "iOS"

    @Suppress("DEPRECATION", "TooGenericExceptionCaught")
    override fun open(draft: SupportMailDraft): SupportMailOpenResult =
        try {
            val url = NSURL.URLWithString(draft.toMailtoUri()) ?: return SupportMailOpenResult.FAILED
            val application = UIApplication.sharedApplication
            if (!application.canOpenURL(url)) {
                SupportMailOpenResult.UNAVAILABLE
            } else if (application.openURL(url)) {
                SupportMailOpenResult.OPENED
            } else {
                SupportMailOpenResult.FAILED
            }
        } catch (exception: Exception) {
            Napier.e("Failed to open support mail app: ${exception.message}", tag = "SupportMail")
            SupportMailOpenResult.FAILED
        }

    @Suppress("TooGenericExceptionCaught")
    override fun copyToClipboard(text: String): Boolean =
        try {
            UIPasteboard.generalPasteboard.string = text
            true
        } catch (exception: Exception) {
            Napier.e("Failed to copy support email: ${exception.message}", tag = "SupportMail")
            false
        }
}
