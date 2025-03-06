package com.nexters.bandalart.feature.complete.viewmodel

import com.eygraber.uri.Uri

sealed interface CompleteUiEvent {
    data object NavigateBack : CompleteUiEvent
    data class SaveBandalart(val imageUri: Uri) : CompleteUiEvent
    data class ShareBandalart(val imageUri: Uri) : CompleteUiEvent
}
