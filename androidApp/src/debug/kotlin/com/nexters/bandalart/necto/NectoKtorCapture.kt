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
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.save
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.OutgoingContent
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/**
 * Ktor 요청과 응답을 Necto Network 패널로 보내는 시제품 플러그인.
 *
 * 엔진과 무관하게 동작하도록 Ktor `Send` 단계에서 기록한다. 응답 본문을 기록하려고
 * [save]로 본문을 메모리에 읽어 두므로, 끝나지 않는 스트림에는 쓰지 않는다.
 * 기록 중 생긴 오류는 삼키고, 요청 자체의 결과와 예외만 앱에 그대로 전달한다.
 */
fun nectoKtorCapture(network: NectoNetworkReporting): ClientPlugin<Unit> =
    createClientPlugin("NectoKtorCapture") {
        on(Send) { request ->
            val startedNanos = System.nanoTime()
            val pending = safely { pendingRecord(request) }
            pending?.let { safely { network.report(it) } }

            val call = try {
                proceed(request).save()
            } catch (error: Throwable) {
                pending?.let { record ->
                    safely { network.report(record.failed(error, elapsedSince(startedNanos))) }
                }
                throw error
            }

            pending?.let { record ->
                safely { network.report(record.completed(call, elapsedSince(startedNanos))) }
            }
            call
        }
    }

private fun pendingRecord(request: HttpRequestBuilder): NectoNetworkRecord {
    val body = request.body as? OutgoingContent
    val headers = request.headers.build().toRecordHeaders().toMutableMap()
    // Send 단계에서는 본문 헤더가 아직 요청 헤더에 합쳐지지 않았다.
    body?.contentType?.let { headers[HttpHeaders.ContentType] = it.toString() }
    body?.contentLength?.let { headers[HttpHeaders.ContentLength] = it.toString() }
    return NectoNetworkRecord(
        id = UUID.randomUUID().toString(),
        method = request.method.value,
        url = request.url.buildString(),
        startedAtMilliseconds = System.currentTimeMillis().toDouble(),
        state = NectoNetworkRecord.State.PENDING,
        requestHeaders = headers,
        requestBody = (body as? OutgoingContent.ByteArrayContent)?.let { content ->
            NectoNetworkRecord.Body.of(content.bytes(), content.contentType?.toString())
        },
    )
}

private suspend fun NectoNetworkRecord.completed(call: HttpClientCall, duration: Double): NectoNetworkRecord {
    // 본문을 읽지 못해도 상태와 헤더는 남긴다.
    val bytes = try {
        call.response.bodyAsBytes()
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        null
    }
    return copy(
        state = NectoNetworkRecord.State.COMPLETED,
        statusCode = call.response.status.value,
        durationMilliseconds = duration,
        responseByteCount = bytes?.size?.toLong(),
        responseHeaders = call.response.headers.toRecordHeaders(),
        responseBody = bytes?.let {
            NectoNetworkRecord.Body.of(it, call.response.headers[HttpHeaders.ContentType])
        },
    )
}

private fun NectoNetworkRecord.failed(error: Throwable, duration: Double): NectoNetworkRecord =
    copy(
        state = NectoNetworkRecord.State.FAILED,
        durationMilliseconds = duration,
        errorSummary = errorSummary(error),
    )

/** HttpTimeout은 타임아웃을 원인으로 담은 취소로 알리므로, 원인을 먼저 본다. */
private fun errorSummary(error: Throwable): String {
    val timeout = generateSequence(error) { it.cause }.firstOrNull { it is HttpRequestTimeoutException }
    return when {
        timeout != null -> timeout.toString()
        error is CancellationException -> "Cancelled"
        else -> error.toString()
    }
}

private fun elapsedSince(startedNanos: Long): Double = (System.nanoTime() - startedNanos) / 1_000_000.0

private fun Headers.toRecordHeaders(): Map<String, String> =
    entries().associate { (name, values) -> name to values.joinToString(", ") }

/** 기록은 앱 요청에 영향을 주면 안 되므로 실패해도 넘어간다. 취소는 그대로 전달한다. */
private inline fun <T> safely(block: () -> T): T? =
    try {
        block()
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        null
    }
