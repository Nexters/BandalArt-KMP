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

package com.nexters.bandalart.feature.home.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.nexters.bandalart.core.common.Language
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartEmojiEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartMainCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartSubCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartTaskCellEntity
import com.nexters.bandalart.core.domain.entity.ThemeMode
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import com.nexters.bandalart.core.domain.repository.InAppUpdateRepository
import com.nexters.bandalart.core.domain.repository.SettingsRepository
import com.nexters.bandalart.feature.complete.CompleteScreen
import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.home.mapper.toUiModel
import com.nexters.bandalart.feature.home.model.BandalartUiModel
import com.nexters.bandalart.feature.home.model.CellType
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@AssistedInject
class HomePresenter(
    @Assisted private val navigator: Navigator,
    private val bandalartRepository: BandalartRepository,
    private val inAppUpdateRepository: InAppUpdateRepository,
    private val settingsRepository: SettingsRepository,
) : Presenter<HomeScreen.State> {
    @Composable
    override fun present(): HomeScreen.State {
        var bandalartList by remember { mutableStateOf(persistentListOf<BandalartUiModel>()) }
        var bandalartData by remember { mutableStateOf<BandalartUiModel?>(null) }
        var bandalartCellData by remember { mutableStateOf<BandalartCellEntity?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var isBandalartCompleted by remember { mutableStateOf(false) }
        var isCreatingEmptyBandalart by remember { mutableStateOf(false) }
        var bottomSheet by rememberRetained { mutableStateOf<HomeScreen.BottomSheetState?>(null) }
        var dialog by rememberRetained { mutableStateOf<HomeScreen.DialogState?>(null) }
        var isDropDownMenuOpened by remember { mutableStateOf(false) }
        var imageRequest by remember { mutableStateOf<HomeScreen.ImageRequest?>(null) }
        var updateVersionCode by remember { mutableStateOf<Int?>(null) }
        var effect by remember { mutableStateOf<HomeScreen.Effect?>(null) }
        var requestedCompletionId by remember { mutableStateOf<Long?>(null) }
        val completingTaskCellIds = remember { mutableSetOf<Long>() }
        val pendingEffects = remember { ArrayDeque<HomeScreen.Effect>() }
        val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
        val scope = rememberCoroutineScope()

        fun emitEffect(newEffect: HomeScreen.Effect) {
            if (effect == newEffect || pendingEffects.lastOrNull() == newEffect) return

            if (effect == null) {
                effect = newEffect
            } else {
                pendingEffects.addLast(newEffect)
            }
        }

        fun consumeEffect() {
            effect = pendingEffects.removeFirstOrNull()
        }

        suspend fun loadBandalart(
            bandalartId: Long,
            isCompleted: Boolean = false,
        ) {
            isLoading = true
            bandalartData = bandalartRepository.getBandalart(bandalartId).toUiModel()

            val mainCell = bandalartRepository.getBandalartMainCell(bandalartId)
            val subCells = bandalartRepository.getChildCells(mainCell?.id ?: 0L)
            val children =
                subCells.map { subCell ->
                    val taskCells = bandalartRepository.getChildCells(subCell.id)
                    BandalartCellEntity(
                        id = subCell.id,
                        title = subCell.title,
                        description = subCell.description,
                        dueDate = subCell.dueDate,
                        isCompleted = subCell.isCompleted,
                        parentId = subCell.parentId,
                        children =
                            taskCells.map { taskCell ->
                                BandalartCellEntity(
                                    id = taskCell.id,
                                    title = taskCell.title,
                                    description = taskCell.description,
                                    dueDate = taskCell.dueDate,
                                    isCompleted = taskCell.isCompleted,
                                    parentId = taskCell.parentId,
                                    children = emptyList(),
                                )
                            },
                    )
                }

            bandalartCellData =
                BandalartCellEntity(
                    id = mainCell?.id ?: 0L,
                    title = mainCell?.title,
                    description = mainCell?.description,
                    dueDate = mainCell?.dueDate,
                    isCompleted = mainCell?.isCompleted ?: false,
                    parentId = mainCell?.parentId,
                    children = children,
                )
            isBandalartCompleted = isCompleted
            isLoading = false
        }

        suspend fun createInitialBandalart() {
            if (isCreatingEmptyBandalart) return

            isCreatingEmptyBandalart = true
            bandalartRepository.createBandalart()?.let { bandalart ->
                bandalartRepository.setRecentBandalartId(bandalart.id)
                bandalartRepository.upsertBandalartId(bandalart.id, bandalart.isCompleted)
                loadBandalart(bandalart.id)
            }
        }

        suspend fun createBandalart() {
            if (bandalartList.size >= MAX_BANDALART_COUNT) {
                emitEffect(HomeScreen.Effect.ShowLimitToast)
                return
            }

            bandalartRepository.createBandalart()?.let { bandalart ->
                bottomSheet = null
                bandalartRepository.setRecentBandalartId(bandalart.id)
                bandalartRepository.upsertBandalartId(bandalart.id, bandalart.isCompleted)
                loadBandalart(bandalart.id)
                emitEffect(HomeScreen.Effect.ShowCreateSnackbar)
            }
        }

        fun openBandalartList() {
            val currentBandalartId = bandalartData?.id ?: return
            bottomSheet = HomeScreen.BottomSheetState.BandalartList(currentBandalartId)
        }

        fun openEmoji() {
            val currentBandalart = bandalartData ?: return
            val mainCellId = bandalartCellData?.id ?: return
            bottomSheet =
                HomeScreen.BottomSheetState.Emoji(
                    bandalartId = currentBandalart.id,
                    cellId = mainCellId,
                    currentEmoji = currentBandalart.profileEmoji,
                )
        }

        fun openCell(
            cellType: CellType,
            isMainCellTitleEmpty: Boolean,
            cellData: BandalartCellEntity,
        ) {
            if (cellType != CellType.MAIN && isMainCellTitleEmpty) {
                emitEffect(HomeScreen.Effect.ShowMainGoalToast)
                return
            }

            val currentBandalart = bandalartData ?: return
            bottomSheet =
                HomeScreen.BottomSheetState.Cell(
                    cellType = cellType,
                    initialCellData = cellData,
                    cellData = cellData,
                    initialBandalartData = currentBandalart,
                    bandalartData = currentBandalart,
                )
        }

        fun updateCellTitle(
            title: String,
            language: Language,
        ) {
            val currentSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell ?: return
            val maxLength =
                when (language) {
                    Language.KOREAN, Language.JAPANESE -> 15
                    Language.ENGLISH -> 24
                }
            val validatedTitle =
                title.takeIf { it.length <= maxLength } ?: currentSheet.cellData.title.orEmpty()
            bottomSheet =
                currentSheet.copy(
                    cellData = currentSheet.cellData.copy(title = validatedTitle),
                )
        }

        fun updateDescription(description: String) {
            val currentSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell ?: return
            val validatedDescription =
                description.takeIf { it.length <= MAX_DESCRIPTION_LENGTH }
                    ?: currentSheet.cellData.description
            bottomSheet =
                currentSheet.copy(
                    cellData = currentSheet.cellData.copy(description = validatedDescription),
                )
        }

        fun updateDueDate(dueDate: String) {
            val currentSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell ?: return
            bottomSheet =
                currentSheet.copy(
                    cellData = currentSheet.cellData.copy(dueDate = dueDate),
                    isDatePickerOpened = false,
                )
        }

        fun updateCompletion(isCompleted: Boolean) {
            val currentSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell ?: return
            bottomSheet =
                currentSheet.copy(
                    cellData = currentSheet.cellData.copy(isCompleted = isCompleted),
                )
        }

        fun updateEmojiDraft(emoji: String) {
            val currentSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell ?: return
            bottomSheet =
                currentSheet.copy(
                    bandalartData = currentSheet.bandalartData.copy(profileEmoji = emoji),
                    isEmojiPickerOpened = false,
                )
        }

        fun updateThemeColor(
            mainColor: String,
            subColor: String,
        ) {
            val currentSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell ?: return
            bottomSheet =
                currentSheet.copy(
                    bandalartData =
                        currentSheet.bandalartData.copy(
                            mainColor = mainColor,
                            subColor = subColor,
                        ),
                )
        }

        suspend fun saveCell() {
            val currentSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell ?: return
            val currentBandalart = currentSheet.bandalartData
            val currentCell = currentSheet.cellData
            val title = currentCell.title?.trim()
            val dueDate = currentCell.dueDate?.ifEmpty { null }

            when (currentSheet.cellType) {
                CellType.MAIN ->
                    bandalartRepository.updateBandalartMainCell(
                        bandalartId = currentBandalart.id,
                        cellId = currentCell.id,
                        updateBandalartMainCellEntity =
                            UpdateBandalartMainCellEntity(
                                title = title,
                                description = currentCell.description,
                                dueDate = dueDate,
                                profileEmoji = currentBandalart.profileEmoji,
                                mainColor = currentBandalart.mainColor,
                                subColor = currentBandalart.subColor,
                            ),
                    )

                CellType.SUB ->
                    bandalartRepository.updateBandalartSubCell(
                        bandalartId = currentBandalart.id,
                        cellId = currentCell.id,
                        updateBandalartSubCellEntity =
                            UpdateBandalartSubCellEntity(
                                title = title,
                                description = currentCell.description,
                                dueDate = dueDate,
                            ),
                    )

                CellType.TASK ->
                    bandalartRepository.updateBandalartTaskCell(
                        bandalartId = currentBandalart.id,
                        cellId = currentCell.id,
                        updateBandalartTaskCellEntity =
                            UpdateBandalartTaskCellEntity(
                                title = title,
                                description = currentCell.description,
                                dueDate = dueDate,
                                isCompleted = currentCell.isCompleted,
                            ),
                    )
            }
            bottomSheet = null
        }

        suspend fun completeTask(requestedCell: BandalartCellEntity) {
            val currentBandalart = bandalartData ?: return
            val currentCell = bandalartCellData?.findTaskCell(requestedCell.id) ?: return
            if (currentCell.title.isNullOrBlank() || currentCell.isCompleted) return
            if (!completingTaskCellIds.add(currentCell.id)) return

            try {
                bandalartRepository.updateBandalartTaskCell(
                    bandalartId = currentBandalart.id,
                    cellId = currentCell.id,
                    updateBandalartTaskCellEntity =
                        UpdateBandalartTaskCellEntity(
                            title = currentCell.title,
                            description = currentCell.description,
                            dueDate = currentCell.dueDate,
                            isCompleted = true,
                        ),
                )
                bandalartCellData = bandalartCellData?.completeTask(currentCell.id)
                emitEffect(HomeScreen.Effect.PlayTaskCompletionHaptic(currentCell.id))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Napier.e("Failed to complete task cell", exception, tag = "HomePresenter")
            } finally {
                completingTaskCellIds.remove(currentCell.id)
            }
        }

        suspend fun updateBandalartEmoji(
            bandalartId: Long,
            cellId: Long,
            emoji: String?,
        ) {
            bandalartRepository.updateBandalartEmoji(
                bandalartId = bandalartId,
                cellId = cellId,
                updateBandalartEmojiEntity = UpdateBandalartEmojiEntity(profileEmoji = emoji),
            )
            bottomSheet = null
        }

        suspend fun deleteBandalart(bandalartId: Long) {
            isLoading = true
            bandalartRepository.deleteBandalart(bandalartId)
            bandalartRepository.deleteCompletedBandalartId(bandalartId)
            dialog = null
            bottomSheet = null
            isDropDownMenuOpened = false
            emitEffect(HomeScreen.Effect.ShowDeleteSnackbar)
        }

        suspend fun deleteCell(cellId: Long) {
            bandalartRepository.deleteBandalartCell(cellId)
            dialog = null
            bottomSheet = null
        }

        LaunchedEffect(Unit) {
            bandalartRepository.getBandalartList().collect { entities ->
                val currentList = entities.map { it.toUiModel() }
                bandalartList = currentList.toPersistentList()

                val previousList = bandalartRepository.getPrevBandalartList()
                val newlyCompletedIds =
                    currentList
                        .filter { bandalart ->
                            val previous = previousList.find { it.first == bandalart.id }
                            previous != null && !previous.second && bandalart.isCompleted
                        }.map { it.id }

                if (newlyCompletedIds.isNotEmpty()) {
                    loadBandalart(
                        bandalartId = newlyCompletedIds.first(),
                        isCompleted = true,
                    )
                    return@collect
                }

                currentList.forEach { bandalart ->
                    bandalartRepository.upsertBandalartId(
                        bandalartId = bandalart.id,
                        isCompleted = bandalart.isCompleted,
                    )
                }

                if (currentList.isEmpty()) {
                    createInitialBandalart()
                    return@collect
                }

                isCreatingEmptyBandalart = false
                val recentBandalartId = bandalartRepository.getRecentBandalartId()
                val selectedId =
                    recentBandalartId.takeIf { recentId ->
                        currentList.any { it.id == recentId }
                    } ?: currentList.first().id
                loadBandalart(selectedId)
            }
        }

        LaunchedEffect(isBandalartCompleted, bandalartData?.id) {
            val completedBandalart = bandalartData
            if (
                isBandalartCompleted &&
                completedBandalart != null &&
                completedBandalart.titleText.isNotEmpty() &&
                requestedCompletionId != completedBandalart.id
            ) {
                requestedCompletionId = completedBandalart.id
                imageRequest =
                    HomeScreen.ImageRequest.Complete(
                        bandalartId = completedBandalart.id,
                        bandalartTitle = completedBandalart.titleText,
                        bandalartProfileEmoji = completedBandalart.profileEmoji.orEmpty(),
                    )
            }
        }

        return HomeScreen.State(
            bandalartList = bandalartList,
            bandalartData = bandalartData,
            bandalartCellData = bandalartCellData,
            isLoading = isLoading,
            isBandalartCompleted = isBandalartCompleted,
            bottomSheet = bottomSheet,
            dialog = dialog,
            isDropDownMenuOpened = isDropDownMenuOpened,
            imageRequest = imageRequest,
            updateVersionCode = updateVersionCode,
            themeMode = themeMode,
            effect = effect,
        ) { event ->
            when (event) {
                is HomeScreen.Event.SelectBandalart -> {
                    scope.launch {
                        bandalartRepository.setRecentBandalartId(event.bandalartId)
                        loadBandalart(event.bandalartId)
                        bottomSheet = null
                    }
                }

                HomeScreen.Event.AddBandalart -> scope.launch { createBandalart() }
                HomeScreen.Event.OpenBandalartList -> openBandalartList()
                HomeScreen.Event.OpenSettings -> {
                    bottomSheet = HomeScreen.BottomSheetState.Settings
                }
                HomeScreen.Event.OpenEmoji -> openEmoji()
                is HomeScreen.Event.OpenCell ->
                    openCell(
                        cellType = event.cellType,
                        isMainCellTitleEmpty = event.isMainCellTitleEmpty,
                        cellData = event.cellData,
                    )

                is HomeScreen.Event.CompleteTask -> {
                    scope.launch { completeTask(event.cellData) }
                }

                HomeScreen.Event.OpenBandalartDeleteDialog -> {
                    dialog = HomeScreen.DialogState.BandalartDelete
                }

                HomeScreen.Event.OpenCellDeleteDialog -> {
                    val currentSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell
                    currentSheet?.let { sheet ->
                        dialog =
                            HomeScreen.DialogState.CellDelete(
                                cellId = sheet.cellData.id,
                                cellType = sheet.cellType,
                                cellTitle = sheet.cellData.title,
                            )
                    }
                }

                HomeScreen.Event.OpenDropDownMenu -> isDropDownMenuOpened = true
                HomeScreen.Event.DismissDropDownMenu -> isDropDownMenuOpened = false
                HomeScreen.Event.DismissBottomSheet -> bottomSheet = null
                HomeScreen.Event.DismissDialog -> dialog = null
                is HomeScreen.Event.UpdateCellTitle ->
                    updateCellTitle(
                        title = event.title,
                        language = event.language,
                    )

                is HomeScreen.Event.UpdateDescription -> updateDescription(event.description)
                is HomeScreen.Event.UpdateDueDate -> updateDueDate(event.dueDate)
                is HomeScreen.Event.UpdateCompletion -> updateCompletion(event.isCompleted)
                is HomeScreen.Event.UpdateEmojiDraft -> updateEmojiDraft(event.emoji)
                is HomeScreen.Event.UpdateThemeColor ->
                    updateThemeColor(
                        mainColor = event.mainColor,
                        subColor = event.subColor,
                    )

                HomeScreen.Event.OpenDatePicker -> {
                    val currentSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell
                    currentSheet?.let { bottomSheet = it.copy(isDatePickerOpened = true) }
                }

                HomeScreen.Event.OpenEmojiPicker -> {
                    val currentSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell
                    currentSheet?.let { bottomSheet = it.copy(isEmojiPickerOpened = true) }
                }

                HomeScreen.Event.SaveCell -> scope.launch { saveCell() }
                is HomeScreen.Event.UpdateBandalartEmoji -> {
                    scope.launch {
                        updateBandalartEmoji(
                            bandalartId = event.bandalartId,
                            cellId = event.cellId,
                            emoji = event.emoji,
                        )
                    }
                }

                is HomeScreen.Event.DeleteBandalart -> {
                    scope.launch { deleteBandalart(event.bandalartId) }
                }

                is HomeScreen.Event.DeleteCell -> scope.launch { deleteCell(event.cellId) }
                HomeScreen.Event.ConsumeEffect -> consumeEffect()
                is HomeScreen.Event.SelectThemeMode -> {
                    scope.launch { settingsRepository.setThemeMode(event.themeMode) }
                }

                HomeScreen.Event.ContactSupport -> emitEffect(HomeScreen.Effect.OpenSupportMail)
                HomeScreen.Event.RequestShare -> imageRequest = HomeScreen.ImageRequest.Share
                HomeScreen.Event.RequestSave -> {
                    isDropDownMenuOpened = false
                    imageRequest = HomeScreen.ImageRequest.Save
                }

                HomeScreen.Event.ImageRequestHandled -> imageRequest = null
                is HomeScreen.Event.CaptureFinished -> {
                    val request = imageRequest as? HomeScreen.ImageRequest.Complete
                    if (request != null) {
                        imageRequest = null
                        isBandalartCompleted = false
                        navigator.goTo(
                            CompleteScreen(
                                bandalartId = request.bandalartId,
                                bandalartTitle = request.bandalartTitle,
                                bandalartProfileEmoji = request.bandalartProfileEmoji,
                                bandalartChartImageUri = event.imageUri,
                            ),
                        )
                    }
                }

                is HomeScreen.Event.CheckForUpdate -> {
                    scope.launch {
                        if (!inAppUpdateRepository.isUpdateAlreadyRejected(event.versionCode)) {
                            updateVersionCode = event.versionCode
                        }
                    }
                }

                HomeScreen.Event.CancelUpdate -> {
                    scope.launch {
                        updateVersionCode?.let { versionCode ->
                            inAppUpdateRepository.setLastRejectedUpdateVersion(versionCode)
                        }
                        updateVersionCode = null
                    }
                }
            }
        }
    }

    @CircuitInject(HomeScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(
            @Assisted navigator: Navigator,
        ): HomePresenter
    }

    private companion object {
        const val MAX_BANDALART_COUNT = 5
        const val MAX_DESCRIPTION_LENGTH = 1000
    }
}

private fun BandalartCellEntity.findTaskCell(cellId: Long): BandalartCellEntity? =
    children
        .asSequence()
        .flatMap { subCell -> subCell.children.asSequence() }
        .firstOrNull { taskCell -> taskCell.id == cellId }

private fun BandalartCellEntity.completeTask(cellId: Long): BandalartCellEntity =
    if (id == cellId) {
        copy(isCompleted = true)
    } else {
        copy(children = children.map { child -> child.completeTask(cellId) })
    }
