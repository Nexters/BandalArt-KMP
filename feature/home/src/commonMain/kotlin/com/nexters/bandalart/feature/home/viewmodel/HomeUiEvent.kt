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

import androidx.compose.ui.graphics.ImageBitmap

sealed interface HomeUiEvent {
    data class NavigateToComplete(
        val id: Long,
        val title: String,
        val profileEmoji: String,
        val bandalartChart: String,
    ) : HomeUiEvent

    data class ShowSnackbar(
        val message: String
    ) : HomeUiEvent

    data class ShowToast(
        val message: String
    ) : HomeUiEvent

    data class SaveBandalart(
        val bitmap: ImageBitmap
    ) : HomeUiEvent

    data class ShareBandalart(
        val bitmap: ImageBitmap
    ) : HomeUiEvent

    data class CaptureBandalart(
        val bitmap: ImageBitmap
    ) : HomeUiEvent

    data object ShowAppVersion : HomeUiEvent
}
