package com.nexters.bandalart.core.common

import io.github.aakira.napier.Napier
import platform.Foundation.NSBundle

actual class AppVersionProvider {
    actual fun getAppVersion(): String {
        return try {
            NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String
                ?: "Unknown"
        } catch (e: Exception) {
            Napier.e("Failed to get app version", e, tag = "AppVersion")
            "Unknown"
        }
    }
}
