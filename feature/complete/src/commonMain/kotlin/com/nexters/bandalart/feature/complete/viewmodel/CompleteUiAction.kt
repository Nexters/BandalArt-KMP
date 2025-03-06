package com.nexters.bandalart.feature.complete.viewmodel

sealed interface CompleteUiAction {
    data object OnBackButtonClick : CompleteUiAction
    data object OnSaveButtonClick : CompleteUiAction
    data object OnShareButtonClick : CompleteUiAction
}
