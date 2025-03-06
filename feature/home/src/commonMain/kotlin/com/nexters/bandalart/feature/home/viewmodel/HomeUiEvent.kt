package com.nexters.bandalart.feature.home.viewmodel

import androidx.compose.ui.graphics.ImageBitmap

sealed interface HomeUiEvent {
    data class NavigateToComplete(
        val id: Long,
        val title: String,
        val profileEmoji: String,
        val bandalartChart: String,
    ) : HomeUiEvent

    data class ShowSnackbar(val message: String) : HomeUiEvent
    data class ShowToast(val message: String) : HomeUiEvent
    data class SaveBandalart(val bitmap: ImageBitmap) : HomeUiEvent
    data class ShareBandalart(val bitmap: ImageBitmap) : HomeUiEvent
    data class CaptureBandalart(val bitmap: ImageBitmap) : HomeUiEvent
    data object ShowAppVersion : HomeUiEvent
}
