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

package com.nexters.bandalart.widget

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BandalartWidgetRefreshRunnerTest {
    @Test
    fun `latest progress wins when background and database refresh overlap`() =
        runBlocking {
            var currentProgress = 0
            var renderedProgress = -1
            val staleRefreshStarted = CompletableDeferred<Unit>()
            val releaseStaleRefresh = CompletableDeferred<Unit>()
            val runner =
                BandalartWidgetRefreshRunner {
                    val capturedProgress = currentProgress
                    if (capturedProgress == 0) {
                        staleRefreshStarted.complete(Unit)
                        releaseStaleRefresh.await()
                    }
                    renderedProgress = capturedProgress
                }

            val backgroundRefresh = launch { runner.refresh() }
            staleRefreshStarted.await()
            currentProgress = 4
            val databaseRefresh = launch { runner.refresh() }
            yield()
            releaseStaleRefresh.complete(Unit)
            joinAll(backgroundRefresh, databaseRefresh)

            assertEquals(4, renderedProgress)
        }
}
