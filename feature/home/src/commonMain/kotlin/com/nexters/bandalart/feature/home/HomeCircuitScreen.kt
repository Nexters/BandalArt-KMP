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

package com.nexters.bandalart.feature.home

import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.navigation.CommonParcelize
import com.nexters.bandalart.feature.home.model.BandalartUiModel
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import com.slack.circuit.runtime.screen.StaticScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@CommonParcelize
data object HomeScreen : ParcelableScreen, StaticScreen {
    data class State(
        val bandalartList: ImmutableList<BandalartUiModel> = persistentListOf(),
        val bandalartData: BandalartUiModel? = null,
        val bandalartCellData: BandalartCellEntity? = null,
        val isLoading: Boolean = true,
        val isBandalartCompleted: Boolean = false,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
        data class SelectBandalart(
            val bandalartId: Long,
        ) : Event
    }
}
