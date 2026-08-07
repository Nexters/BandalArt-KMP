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

import com.nexters.bandalart.core.common.Language
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.ThemeMode
import com.nexters.bandalart.core.navigation.CommonParcelize
import com.nexters.bandalart.feature.home.model.BandalartUiModel
import com.nexters.bandalart.feature.home.model.CellType
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
        val bottomSheet: BottomSheetState? = null,
        val dialog: DialogState? = null,
        val isDropDownMenuOpened: Boolean = false,
        val imageRequest: ImageRequest? = null,
        val updateVersionCode: Int? = null,
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val effect: Effect? = null,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed interface BottomSheetState {
        data class Cell(
            val cellType: CellType,
            val initialCellData: BandalartCellEntity,
            val cellData: BandalartCellEntity,
            val initialBandalartData: BandalartUiModel,
            val bandalartData: BandalartUiModel,
            val isDatePickerOpened: Boolean = false,
            val isEmojiPickerOpened: Boolean = false,
        ) : BottomSheetState

        data class BandalartList(
            val currentBandalartId: Long,
        ) : BottomSheetState

        data class Emoji(
            val bandalartId: Long,
            val cellId: Long,
            val currentEmoji: String?,
        ) : BottomSheetState

        data object Settings : BottomSheetState
    }

    sealed interface DialogState {
        data object BandalartDelete : DialogState

        data class CellDelete(
            val cellId: Long,
            val cellType: CellType,
            val cellTitle: String?,
        ) : DialogState
    }

    sealed interface Effect {
        data object ShowCreateSnackbar : Effect

        data object ShowDeleteSnackbar : Effect

        data object ShowLimitToast : Effect

        data object ShowMainGoalToast : Effect

        data object OpenSupportMail : Effect

        data class PlayTaskCompletionHaptic(
            val taskCellId: Long,
        ) : Effect
    }

    sealed interface ImageRequest {
        data object Share : ImageRequest

        data object Save : ImageRequest

        data class Complete(
            val bandalartId: Long,
            val bandalartTitle: String,
            val bandalartProfileEmoji: String,
        ) : ImageRequest
    }

    sealed interface Event : CircuitUiEvent {
        data class SelectBandalart(
            val bandalartId: Long,
        ) : Event

        data object AddBandalart : Event

        data object OpenBandalartList : Event

        data object OpenEmoji : Event

        data class OpenCell(
            val cellType: CellType,
            val isMainCellTitleEmpty: Boolean,
            val cellData: BandalartCellEntity,
        ) : Event

        data class CompleteTask(
            val cellData: BandalartCellEntity,
        ) : Event

        data object OpenBandalartDeleteDialog : Event

        data object OpenCellDeleteDialog : Event

        data object OpenDropDownMenu : Event

        data object DismissDropDownMenu : Event

        data object DismissBottomSheet : Event

        data object DismissDialog : Event

        data class UpdateCellTitle(
            val title: String,
            val language: Language,
        ) : Event

        data class UpdateDescription(
            val description: String,
        ) : Event

        data class UpdateDueDate(
            val dueDate: String,
        ) : Event

        data class UpdateCompletion(
            val isCompleted: Boolean,
        ) : Event

        data class UpdateEmojiDraft(
            val emoji: String,
        ) : Event

        data class UpdateThemeColor(
            val mainColor: String,
            val subColor: String,
        ) : Event

        data object OpenDatePicker : Event

        data object OpenEmojiPicker : Event

        data object CloseEmojiPicker : Event

        data object SaveCell : Event

        data class UpdateBandalartEmoji(
            val bandalartId: Long,
            val cellId: Long,
            val emoji: String?,
        ) : Event

        data class DeleteBandalart(
            val bandalartId: Long,
        ) : Event

        data class DeleteCell(
            val cellId: Long,
        ) : Event

        data object ConsumeEffect : Event

        data object OpenSettings : Event

        data class SelectThemeMode(
            val themeMode: ThemeMode,
        ) : Event

        data object ContactSupport : Event

        data object RequestShare : Event

        data object RequestSave : Event

        data object ImageRequestHandled : Event

        data class CaptureFinished(
            val imageUri: String,
        ) : Event

        data class CheckForUpdate(
            val versionCode: Int,
        ) : Event

        data object CancelUpdate : Event
    }
}
