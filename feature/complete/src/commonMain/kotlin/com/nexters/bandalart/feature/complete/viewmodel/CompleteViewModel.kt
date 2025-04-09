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

package com.nexters.bandalart.feature.complete.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.eygraber.uri.Uri
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import com.nexters.bandalart.core.navigation.Route
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CompleteViewModel(
    private val bandalartRepository: BandalartRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<Route.Complete>().bandalartId
    private val title = savedStateHandle.toRoute<Route.Complete>().bandalartTitle
    private val profileEmoji = savedStateHandle.toRoute<Route.Complete>().bandalartProfileEmoji ?: ""
    private val bandalartChartImageUri = savedStateHandle.toRoute<Route.Complete>().bandalartChartImageUri

    private val _uiState = MutableStateFlow(CompleteUiState())
    val uiState: StateFlow<CompleteUiState> = this._uiState.asStateFlow()

    private val _uiEvent = Channel<CompleteUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        initComplete()
        addCompleteBandalartId()
    }

    fun onAction(action: CompleteUiAction) {
        when (action) {
            is CompleteUiAction.OnBackButtonClick -> navigateBack()
            is CompleteUiAction.OnSaveButtonClick -> saveBandalart()
            is CompleteUiAction.OnShareButtonClick -> shareBandalart()
        }
    }

    private fun initComplete() {
        _uiState.update {
            it.copy(
                id = id,
                title = title,
                profileEmoji = profileEmoji,
                bandalartChartImageUri = bandalartChartImageUri,
            )
        }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            _uiEvent.send(CompleteUiEvent.NavigateBack)
        }
    }

    private fun addCompleteBandalartId() {
        viewModelScope.launch {
            bandalartRepository.upsertBandalartId(
                bandalartId = id,
                isCompleted = true,
            )
        }
    }

    private fun saveBandalart() {
        viewModelScope.launch {
            _uiEvent.send(CompleteUiEvent.SaveBandalart(Uri.parse(bandalartChartImageUri)))
        }
    }

    private fun shareBandalart() {
        viewModelScope.launch {
            _uiEvent.send(CompleteUiEvent.ShareBandalart(Uri.parse(bandalartChartImageUri)))
        }
    }
}
