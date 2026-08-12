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

package com.nexters.bandalart.core.database

internal data class DatabaseFileMove(
    val fileName: String,
)

internal object DatabaseFileMigrationPolicy {
    fun plan(
        baseFileName: String,
        sourceFiles: Set<String>,
        destinationFiles: Set<String>,
    ): List<DatabaseFileMove> {
        check(baseFileName !in sourceFiles || baseFileName !in destinationFiles) {
            "Both legacy and App Group databases exist; refusing to choose one implicitly"
        }
        if (baseFileName !in sourceFiles) return emptyList()

        return listOf("$baseFileName-wal", "$baseFileName-shm", baseFileName)
            .filter { fileName -> fileName in sourceFiles && fileName !in destinationFiles }
            .map(::DatabaseFileMove)
    }
}
