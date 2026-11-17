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

package com.nexters.bandalart.necto

import im.toss.necto.plugins.NectoNetworkRecord
import im.toss.necto.plugins.NectoNetworkReporting
import io.ktor.client.call.save
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.Headers
import io.ktor.http.content.OutgoingContent
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/**
 * Ktor 요청과 응답을 Necto Network 패널로 보내는 시제품 플러그인.
 *
 * 엔진과 무관하게 동작하도록 Ktor `Send` 단계에서 기록한다. 응답 본문을 기록하려고
 * [save]로 본문을 메모리에 읽어 두므로, 스트리밍 응답은 끝까지 받은 뒤에 앱에 전달된다.
 */
fun nectoKtorCapture(network: NectoNetworkReporting): ClientPlugin<Unit> =
    createClientPlugin("NectoKtorCapture") {
        on(Send) { request ->
            val startedAt = System.currentTimeMillis()
            val startedNanos = System.nanoTime()
            val pending = NectoNetworkRecord(
                id = UUID.randomUUID().toString(),
                method = request.method.value,
                url = request.url.buildString(),
                startedAtMilliseconds = startedAt.toDouble(),
                state = NectoNetworkRecord.State.PENDING,
                requestHeaders = request.headers.build().toRecordHeaders(),
                requestBody = (request.body as? OutgoingContent.ByteArrayContent)?.let { content ->
                    NectoNetworkRecord.Body.of(content.bytes(), content.contentType?.toString())
                },
            )
            network.report(pending)

            fun elapsed() = (System.nanoTime() - startedNanos) / 1_000_000.0

            try {
                val call = proceed(request).save()
                val bytes = call.response.bodyAsBytes()
                network.report(
                    pending.copy(
                        state = NectoNetworkRecord.State.COMPLETED,
                        statusCode = call.response.status.value,
                        durationMilliseconds = elapsed(),
                        responseByteCount = bytes.size.toLong(),
                        responseHeaders = call.response.headers.toRecordHeaders(),
                        responseBody = NectoNetworkRecord.Body.of(
                            bytes,
                            call.response.headers["Content-Type"],
                        ),
                    ),
                )
                call
            } catch (error: Throwable) {
                network.report(
                    pending.copy(
                        state = NectoNetworkRecord.State.FAILED,
                        durationMilliseconds = elapsed(),
                        errorSummary = if (error is CancellationException) "Cancelled" else error.toString(),
                    ),
                )
                throw error
            }
        }
    }

private fun Headers.toRecordHeaders(): Map<String, String> =
    entries().associate { (name, values) -> name to values.joinToString(", ") }
