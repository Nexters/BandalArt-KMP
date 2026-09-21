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

package com.nexters.bandalart.feature.complete

import com.eygraber.uri.Uri
import com.nexters.bandalart.core.navigation.CommonParcelize
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen

@CommonParcelize
data class CompleteScreen(
    val bandalartId: Long,
    val bandalartTitle: String,
    val bandalartProfileEmoji: String,
    val bandalartChartImageUri: String,
) : ParcelableScreen {
    data class State(
        val id: Long,
        val title: String,
        val profileEmoji: String,
        val bandalartChartImageUri: String,
        val sideEffect: SideEffect?,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed interface SideEffect {
        data class SaveImage(
            val imageUri: Uri
        ) : SideEffect

        data class ShareImage(
            val imageUri: Uri
        ) : SideEffect
    }

    sealed interface Event : CircuitUiEvent {
        data object NavigateBack : Event

        data class SaveBandalart(
            val imageUri: Uri
        ) : Event

        data class ShareBandalart(
            val imageUri: Uri
        ) : Event

        data object ClearSideEffect : Event
    }
}
