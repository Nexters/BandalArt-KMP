/*
 * Copyright 2025 easyhooon
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

package com.nexters.bandalart.necto

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.nexters.bandalart.core.data.backup.BackupHttpClientHooks
import im.toss.necto.android.NectoAndroid
import im.toss.necto.android.NectoAndroidControlPlugin
import im.toss.necto.android.NectoProcessMetricPlugin
import im.toss.necto.plugins.NectoEventsPlugin
import im.toss.necto.plugins.NectoFilesPlugin
import im.toss.necto.plugins.NectoNetworkPlugin

/**
 * Debug 빌드에서만 manifest에 병합되어 Necto를 시작한다.
 * release 계열 빌드 타입에는 이 클래스와 Necto 의존성이 포함되지 않는다.
 */
class NectoInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val context = requireNotNull(context)
        val network = NectoNetworkPlugin()
        // Application.onCreate에서 앱 그래프가 Supabase 클라이언트를 만들기 전에 설정된다.
        BackupHttpClientHooks.configure = { install(nectoKtorCapture(network)) }
        NectoAndroid.start(
            context,
            listOf(
                NectoEventsPlugin(),
                NectoFilesPlugin(context),
                network,
                NectoProcessMetricPlugin(context),
                NectoAndroidControlPlugin(context),
            ),
        )
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
