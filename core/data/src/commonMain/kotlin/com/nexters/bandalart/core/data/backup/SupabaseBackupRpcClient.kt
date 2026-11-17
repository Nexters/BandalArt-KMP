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

package com.nexters.bandalart.core.data.backup

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.ktor.client.HttpClientConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

class SupabaseBackupRpcClient(
    private val client: SupabaseClient,
) : BackupRpcClient {
    override suspend fun getBackup(deviceKey: String): BackupRpcRow? =
        client.postgrest
            .rpc(
                function = "get_device_backup",
                parameters = GetBackupParams(deviceKey),
            ).decodeSingleOrNull()

    override suspend fun putBackup(
        deviceKey: String,
        schemaVersion: Int,
        payload: JsonObject,
        bandalartCount: Int,
    ): BackupRpcMetadataRow =
        client.postgrest
            .rpc(
                function = "put_device_backup",
                parameters =
                    PutBackupParams(
                        deviceKey = deviceKey,
                        schemaVersion = schemaVersion,
                        payload = payload,
                        bandalartCount = bandalartCount,
                    ),
            ).decodeSingle()
}

data class BackupApiConfig(
    val url: String,
    val publishableKey: String,
) {
    val isConfigured: Boolean
        get() = url.startsWith("https://") && publishableKey.isNotBlank()
}

// httpConfig는 supabase-kt에서 @SupabaseInternal로 표시되어 있어 버전이 바뀌면 깨질 수 있다.
@OptIn(SupabaseInternal::class)
fun createSupabaseBackupRpcClient(config: BackupApiConfig): BackupRpcClient {
    check(config.isConfigured) { "Supabase backup API is not configured" }
    return SupabaseBackupRpcClient(
        createSupabaseClient(
            supabaseUrl = config.url,
            supabaseKey = config.publishableKey,
        ) {
            install(Postgrest)
            httpConfig { BackupHttpClientHooks.configure(this) }
        },
    )
}

/**
 * 디버그 도구가 백업 HTTP 클라이언트에 Ktor 설정을 더할 때 쓴다.
 * 기본값은 아무것도 하지 않으며, release 빌드에서는 설정하는 곳이 없다.
 */
object BackupHttpClientHooks {
    var configure: HttpClientConfig<*>.() -> Unit = {}
}

@Serializable
private data class GetBackupParams(
    @SerialName("p_device_key")
    val deviceKey: String,
)

@Serializable
private data class PutBackupParams(
    @SerialName("p_device_key")
    val deviceKey: String,
    @SerialName("p_schema_version")
    val schemaVersion: Int,
    @SerialName("p_payload")
    val payload: JsonObject,
    @SerialName("p_bandalart_count")
    val bandalartCount: Int,
)
