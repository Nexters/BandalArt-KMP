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

package com.nexters.bandalart.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.TextFieldValue
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.bandalart_list_empty_title
import com.nexters.bandalart.feature.home.model.BandalartUiModel
import com.nexters.bandalart.feature.home.ui.bandalart.BandalartBottomSheet
import com.nexters.bandalart.feature.home.ui.bandalart.BandalartEmojiBottomSheet
import com.nexters.bandalart.feature.home.ui.bandalart.BandalartListBottomSheet
import com.nexters.bandalart.feature.home.viewmodel.BottomSheetState
import com.nexters.bandalart.feature.home.viewmodel.HomeUiAction
import com.nexters.bandalart.feature.home.viewmodel.HomeUiState
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeBottomSheets(
    uiState: HomeUiState,
    onHomeUiAction: (HomeUiAction) -> Unit,
) {
    when (uiState.bottomSheet) {
        is BottomSheetState.Cell -> {
            uiState.bandalartData?.let { bandalart ->
                uiState.clickedCellData?.let { cell ->
                    BandalartBottomSheet(
                        bandalartId = bandalart.id,
                        cellType = uiState.clickedCellType,
                        isBlankCell = cell.title.isNullOrEmpty(),
                        cellData = cell,
                        onHomeUiAction = onHomeUiAction,
                        bottomSheetData = BottomSheetState.Cell(
                            initialCellData = uiState.bottomSheet.initialCellData,
                            cellData = uiState.bottomSheet.cellData,
                            initialBandalartData = uiState.bottomSheet.initialBandalartData,
                            bandalartData = uiState.bottomSheet.bandalartData,
                            isDatePickerOpened = uiState.bottomSheet.isDatePickerOpened,
                            isEmojiPickerOpened = uiState.bottomSheet.isEmojiPickerOpened,
                        ),
                    )
                }
            }
        }

        is BottomSheetState.Emoji -> {
            uiState.bandalartData?.let { bandalart ->
                uiState.bandalartCellData?.let { cell ->
                    BandalartEmojiBottomSheet(
                        bandalartId = bandalart.id,
                        cellId = cell.id,
                        currentEmoji = bandalart.profileEmoji,
                        onHomeUiAction = onHomeUiAction,
                    )
                }
            }
        }

        is BottomSheetState.BandalartList -> {
            uiState.bandalartData?.let { bandalart ->
                BandalartListBottomSheet(
                    bandalartList = updateBandalartListTitles(uiState.bandalartList).toImmutableList(),
                    currentBandalartId = bandalart.id,
                    onHomeUiAction = onHomeUiAction,
                )
            }
        }

        null -> {}
    }
}

@Composable
private fun updateBandalartListTitles(list: List<BandalartUiModel>): List<BandalartUiModel> {
    var counter = 1
    return list.map { item ->
        if (!item.hasTitleText) {
            val updatedTitle = stringResource(Res.string.bandalart_list_empty_title, counter)
            counter += 1
            item.copy(
                title = TextFieldValue(updatedTitle),
                isGeneratedTitle = true,
            )
        } else {
            item
        }
    }
}
