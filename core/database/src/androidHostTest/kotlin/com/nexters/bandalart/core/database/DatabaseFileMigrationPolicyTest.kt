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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DatabaseFileMigrationPolicyTest {
    @Test
    fun movesSqliteSidecarsBeforeBaseFile() {
        val moves =
            DatabaseFileMigrationPolicy.plan(
                baseFileName = "bandalart.db",
                sourceFiles = setOf("bandalart.db", "bandalart.db-wal", "bandalart.db-shm"),
                destinationFiles = emptySet(),
            )

        assertEquals(
            listOf(
                DatabaseFileMove("bandalart.db-wal"),
                DatabaseFileMove("bandalart.db-shm"),
                DatabaseFileMove("bandalart.db"),
            ),
            moves,
        )
    }

    @Test
    fun refusesToIgnoreLegacyDatabaseWhenBothBaseFilesExist() {
        assertThrows(IllegalStateException::class.java) {
            DatabaseFileMigrationPolicy.plan(
                baseFileName = "bandalart.db",
                sourceFiles = setOf("bandalart.db", "bandalart.db-wal"),
                destinationFiles = setOf("bandalart.db"),
            )
        }
    }

    @Test
    fun resumesPartialMigrationWithoutReplacingExistingSidecar() {
        val moves =
            DatabaseFileMigrationPolicy.plan(
                baseFileName = "bandalart.db",
                sourceFiles = setOf("bandalart.db", "bandalart.db-wal", "bandalart.db-shm"),
                destinationFiles = setOf("bandalart.db-wal"),
            )

        assertEquals(
            listOf(
                DatabaseFileMove("bandalart.db-shm"),
                DatabaseFileMove("bandalart.db"),
            ),
            moves,
        )
    }

    @Test
    fun startsNewSharedDatabaseWhenNoLegacyBaseFileExists() {
        val moves =
            DatabaseFileMigrationPolicy.plan(
                baseFileName = "bandalart.db",
                sourceFiles = setOf("bandalart.db-wal"),
                destinationFiles = emptySet(),
            )

        assertEquals(emptyList<DatabaseFileMove>(), moves)
    }
}
