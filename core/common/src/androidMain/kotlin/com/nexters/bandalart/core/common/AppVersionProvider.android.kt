package com.nexters.bandalart.core.common

import android.content.Context

actual class AppVersionProvider(private val context: Context) {
    actual fun getAppVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
