package com.nexters.bandalart.feature.home.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import com.nexters.bandalart.core.common.Locale
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartEmojiEntity
import com.nexters.bandalart.feature.home.model.CellType

sealed interface HomeUiAction {
    // HomeScreen UiAction
    data object OnListClick : HomeUiAction
    data object OnSaveClick : HomeUiAction
    data object OnDeleteClick : HomeUiAction
    data class OnEmojiSelected(
        val bandalartId: Long,
        val cellId: Long,
        val updateBandalartEmojiModel: UpdateBandalartEmojiEntity,
    ) : HomeUiAction

    data object OnShareButtonClick : HomeUiAction
    data object OnAddClick : HomeUiAction
    data object OnMenuClick : HomeUiAction
    data object OnDropDownMenuDismiss : HomeUiAction
    data object OnEmojiClick : HomeUiAction
    data class OnBandalartListItemClick(val key: Long) : HomeUiAction
    data class OnBandalartCellClick(
        val cellType: CellType,
        val isMainCellTitleEmpty: Boolean,
        val cellData: BandalartCellEntity,
    ) : HomeUiAction

    data object OnCloseButtonClick : HomeUiAction
    data object OnAppTitleClick : HomeUiAction

    // BottomSheet UiAction
    data object OnDismiss : HomeUiAction
    data class OnCellTitleUpdate(val title: String, val locale: Locale) : HomeUiAction
    data class OnEmojiSelect(val emoji: String) : HomeUiAction
    data class OnColorSelect(val mainColor: String, val subColor: String) : HomeUiAction
    data object OnDatePickerClick : HomeUiAction
    data class OnDueDateSelect(val date: String) : HomeUiAction
    data class OnDescriptionUpdate(val description: String) : HomeUiAction
    data class OnCompletionUpdate(val isCompleted: Boolean) : HomeUiAction
    data class OnDeleteBandalart(val bandalartId: Long) : HomeUiAction
    data class OnDeleteCell(val cellId: Long) : HomeUiAction
    data object OnCancelClick : HomeUiAction
    data object OnEmojiPickerClick : HomeUiAction
    data object OnDeleteButtonClick : HomeUiAction
    data class OnCompleteButtonClick(
        val bandalartId: Long,
        val cellId: Long,
        val cellType: CellType,
    ) : HomeUiAction
}
