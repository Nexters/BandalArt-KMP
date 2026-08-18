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

import android.app.Application
import android.provider.Settings
import com.nexters.bandalart.core.domain.backup.DeviceBackupKeyProvider
import java.security.MessageDigest

class AndroidDeviceBackupKeyProvider(
    private val application: Application,
    private val appNamespace: String = application.packageName,
) : DeviceBackupKeyProvider {
    private val cachedDeviceKey: String? by lazy {
        Settings.Secure
            .getString(application.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeIf(String::isNotBlank)
            ?.let { ssaid -> deriveDeviceBackupKey(appNamespace, ssaid) }
    }

    override fun getDeviceKey(): String? = cachedDeviceKey
}

internal fun deriveDeviceBackupKey(
    appNamespace: String,
    ssaid: String,
): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest("$appNamespace:$ssaid".encodeToByteArray())
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
