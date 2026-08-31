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

package com.nexters.bandalart.feature.home.ui.bandalart

import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BandalartChartTooltipTest {
    @Test
    fun firstIncompleteTaskWithContentIsSelectedInChartOrder() {
        val mainCell =
            cell(
                id = 1L,
                children =
                    listOf(
                        cell(
                            id = 10L,
                            children =
                                listOf(
                                    cell(id = 11L, title = ""),
                                    cell(id = 12L, title = "완료", isCompleted = true),
                                    cell(id = 13L, title = "첫 미완료"),
                                ),
                        ),
                        cell(
                            id = 20L,
                            children = listOf(cell(id = 21L, title = "다음 미완료")),
                        ),
                    ),
            )

        assertEquals(13L, mainCell.firstIncompleteTaskCellId())
    }

    @Test
    fun noTaskIsSelectedWhenEveryTaskIsBlankOrCompleted() {
        val mainCell =
            cell(
                id = 1L,
                children =
                    listOf(
                        cell(
                            id = 10L,
                            children =
                                listOf(
                                    cell(id = 11L, title = " "),
                                    cell(id = 12L, title = "완료", isCompleted = true),
                                ),
                        ),
                    ),
            )

        assertNull(mainCell.firstIncompleteTaskCellId())
    }

    private fun cell(
        id: Long,
        title: String? = null,
        isCompleted: Boolean = false,
        children: List<BandalartCellEntity> = emptyList(),
    ) = BandalartCellEntity(
        id = id,
        title = title,
        isCompleted = isCompleted,
        children = children,
    )
}
