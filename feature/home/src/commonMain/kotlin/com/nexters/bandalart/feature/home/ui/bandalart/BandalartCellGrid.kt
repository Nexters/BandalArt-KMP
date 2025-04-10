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

package com.nexters.bandalart.feature.home.ui.bandalart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.feature.home.model.BandalartUiModel
import com.nexters.bandalart.feature.home.model.CellType
import com.nexters.bandalart.feature.home.model.dummy.dummyBandalartChartData
import com.nexters.bandalart.feature.home.viewmodel.HomeUiAction
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun BandalartCellGrid(
    bandalartData: BandalartUiModel,
    subCell: SubCell,
    rows: Int,
    cols: Int,
    onHomeUiAction: (HomeUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        var taskIndex = 0
        repeat(rows) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                repeat(cols) { colIndex ->
                    val isSubCell = rowIndex == subCell.subCellRowIndex && colIndex == subCell.subCellColIndex
                    BandalartCell(
                        modifier = Modifier.weight(1f),
                        bandalartData = bandalartData,
                        cellType = if (isSubCell) CellType.SUB else CellType.TASK,
                        cellInfo = CellInfo(
                            isSubCell = isSubCell,
                            colIndex = colIndex,
                            rowIndex = rowIndex,
                            colCnt = cols,
                            rowCnt = rows,
                        ),
                        cellData = if (isSubCell) subCell.subCellData!! else subCell.subCellData!!.children[taskIndex++],
                        onHomeUiAction = onHomeUiAction,
                    )
                }
            }
        }
    }
}

// @ComponentPreview
@Preview
@Composable
private fun BandalartCellGridPreview() {
    val subCellList = persistentListOf(
        SubCell(2, 3, 1, 1, dummyBandalartChartData.children[0]),
        SubCell(3, 2, 1, 0, dummyBandalartChartData.children[1]),
        SubCell(3, 2, 1, 1, dummyBandalartChartData.children[2]),
        SubCell(2, 3, 0, 1, dummyBandalartChartData.children[3]),
    )

    BandalartTheme {
        BandalartCellGrid(
            bandalartData = BandalartUiModel(
                id = 0L,
                mainColor = "#3FFFBA",
                subColor = "#111827",
            ),
            subCell = subCellList[1],
            rows = subCellList[1].rowCnt,
            cols = subCellList[1].colCnt,
            onHomeUiAction = {},
        )
    }
}
