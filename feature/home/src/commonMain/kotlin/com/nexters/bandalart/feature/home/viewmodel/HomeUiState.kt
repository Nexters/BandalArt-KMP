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

package com.nexters.bandalart.feature.home.viewmodel

import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.feature.home.model.BandalartUiModel
import com.nexters.bandalart.feature.home.model.CellType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * HomeUiState
 *
 * @param bandalartList 반다라트 목록
 * @param bandalartData 반다라트 데이터
 * @param bandalartCellData 반다라트 각각의 셀의 데이터
 * @param isShowSkeleton 표의 첫 로딩을 보여주는 스켈레톤 이미지
 * @param bandalartChartUrl 반다라트 차트 이미지 URL
 * @param isBandalartCompleted 반다라트 목표를 달성함
 * @param bottomSheet 바텀시트 상태
 * @param dialog 다이얼로그 상태
 * @param isSharing 공유 요청 상태
 * @param isCapturing 저장 요청 상태
 * @param isDropDownMenuOpened 드롭다운 메뉴가 열려있는지 여부
 * @param clickedCellType 클릭한 반다라트 셀의 타입(메인, 서브, 태스크)
 * @param clickedCellData 클릭한 반다라트 셀의 데이터
 */
data class HomeUiState(
    val bandalartList: ImmutableList<BandalartUiModel> = persistentListOf(),
    val bandalartData: BandalartUiModel? = null,
    val bandalartCellData: BandalartCellEntity? = null,
    val isShowSkeleton: Boolean = false,
    val bandalartChartUrl: String? = null,
    val isBandalartCompleted: Boolean = false,
    val bottomSheet: BottomSheetState? = null,
    val dialog: DialogState? = null,
    val isSharing: Boolean = false,
    val isCapturing: Boolean = false,
    val isDropDownMenuOpened: Boolean = false,
    val clickedCellType: CellType = CellType.MAIN,
    val clickedCellData: BandalartCellEntity? = null,
)

sealed interface BottomSheetState {
    data class Cell(
        val initialCellData: BandalartCellEntity,
        val cellData: BandalartCellEntity,
        val initialBandalartData: BandalartUiModel,
        val bandalartData: BandalartUiModel,
        val isDatePickerOpened: Boolean = false,
        val isEmojiPickerOpened: Boolean = false,
    ) : BottomSheetState

    data class BandalartList(
        val bandalartList: ImmutableList<BandalartUiModel>,
        val currentBandalartId: Long,
    ) : BottomSheetState

    data class Emoji(
        val bandalartId: Long,
        val cellId: Long,
        val currentEmoji: String?,
    ) : BottomSheetState
}

sealed interface DialogState {
    data object BandalartDelete : DialogState

    data class CellDelete(
        val cellType: CellType,
        val cellTitle: String?,
    ) : DialogState
}
