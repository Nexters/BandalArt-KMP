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

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object DeadlineNotificationPermissionHistory {
    fun isBlocked(context: Context): Boolean = preferences(context).getBoolean(KEY_BLOCKED, false)

    fun recordPromptResult(activity: Activity) {
        val granted =
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        val preferences = preferences(activity)
        if (granted) {
            preferences.edit().clear().apply()
            return
        }

        val canShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
        val hadRequestableDenial = preferences.getBoolean(KEY_REQUESTABLE_DENIAL, false)
        preferences
            .edit()
            .apply {
                when {
                    canShowRationale -> putBoolean(KEY_REQUESTABLE_DENIAL, true)
                    hadRequestableDenial -> putBoolean(KEY_BLOCKED, true)
                }
            }.apply()
    }

    private fun preferences(context: Context) = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private const val PREFERENCES_NAME = "deadline_notification_permission"
    private const val KEY_REQUESTABLE_DENIAL = "requestable_denial"
    private const val KEY_BLOCKED = "blocked"
}
