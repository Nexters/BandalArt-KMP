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
import androidx.compose.runtime.snapshots.Snapshot
import com.nexters.bandalart.core.common.Language
import com.nexters.bandalart.core.common.RewardedAdResult
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartEmojiEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartMainCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartSubCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartTaskCellEntity
import com.nexters.bandalart.core.domain.entity.ThemeMode
import com.nexters.bandalart.core.domain.notification.DeadlineNotificationAuthorization
import com.nexters.bandalart.core.domain.notification.DeadlineNotificationAuthorizationStatus
import com.nexters.bandalart.core.domain.notification.DeadlineNotificationLaunchTarget
import com.nexters.bandalart.core.domain.notification.DeadlineReminderReconciler
import com.nexters.bandalart.core.domain.notification.BufferedDeadlineNotificationLaunchTarget
import com.nexters.bandalart.core.domain.notification.NoOpDeadlineNotificationAuthorization
import com.nexters.bandalart.core.domain.notification.NoOpDeadlineReminderReconciler
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import com.nexters.bandalart.core.domain.repository.BandalartSlotRepository
import com.nexters.bandalart.core.domain.repository.InAppUpdateRepository
import com.nexters.bandalart.core.domain.repository.SettingsRepository
import com.nexters.bandalart.core.domain.template.BandalartTemplateId
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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.yield
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AssistedInject
@Suppress("LargeClass")
class HomePresenter(
    @Assisted private val navigator: Navigator,
    private val bandalartRepository: BandalartRepository,
    private val bandalartSlotRepository: BandalartSlotRepository,
    private val inAppUpdateRepository: InAppUpdateRepository,
    private val settingsRepository: SettingsRepository,
    private val deadlineNotificationAuthorization: DeadlineNotificationAuthorization =
        NoOpDeadlineNotificationAuthorization,
    private val deadlineReminderReconciler: DeadlineReminderReconciler = NoOpDeadlineReminderReconciler,
    private val deadlineNotificationLaunchTarget: DeadlineNotificationLaunchTarget =
        BufferedDeadlineNotificationLaunchTarget(),
) : Presenter<HomeScreen.State> {
    @Composable
    override fun present(): HomeScreen.State {
        var bandalartList by remember { mutableStateOf(persistentListOf<BandalartUiModel>()) }
        var loadedBandalart by remember { mutableStateOf<LoadedBandalart?>(null) }
        val bandalartData = loadedBandalart?.bandalartData
        val bandalartCellData = loadedBandalart?.bandalartCellData
        val isBandalartCompleted = loadedBandalart?.isCompleted ?: false
        var newlyCompletedTargetId by remember { mutableStateOf<Long?>(null) }
        var explicitSelectionTargetId by remember { mutableStateOf<Long?>(null) }
        var isExplicitSelectionTargetPending by remember { mutableStateOf(false) }
        var bandalartListRevision by remember { mutableStateOf(0L) }
        var isLoading by remember { mutableStateOf(true) }
        var isCreatingEmptyBandalart by remember { mutableStateOf(false) }
        val rewardedCreateCoordinator = rememberRetained { RewardedCreateCoordinator() }
        var isUpdatingBandalartEmoji by remember { mutableStateOf(false) }
        var bottomSheet by rememberRetained { mutableStateOf<HomeScreen.BottomSheetState?>(null) }
        var dialog by rememberRetained { mutableStateOf<HomeScreen.DialogState?>(null) }
        var isDropDownMenuOpened by remember { mutableStateOf(false) }
        var imageRequest by remember { mutableStateOf<HomeScreen.ImageRequest?>(null) }
        var updateVersionCode by remember { mutableStateOf<Int?>(null) }
        var rewardedAdRequestId by rememberRetained { mutableStateOf<Long?>(null) }
        var rewardedRecoveryChecked by rememberRetained { mutableStateOf(false) }
        var pendingCreationTemplateId by rememberRetained { mutableStateOf<BandalartTemplateId?>(null) }
        var effect by remember { mutableStateOf<HomeScreen.Effect?>(null) }
        var requestedCompletionId by remember { mutableStateOf<Long?>(null) }
        val togglingTaskCellIds = remember { mutableSetOf<Long>() }
        val pendingEffects = remember { ArrayDeque<HomeScreen.Effect>() }
        val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
        val recentEmojis by settingsRepository.recentEmojis.collectAsState(initial = emptyList())
        val deadlineReminderEnabled by settingsRepository.deadlineReminderEnabled.collectAsState(initial = false)
        val deadlineReminderSchedulingHealth by deadlineReminderReconciler.schedulingHealth.collectAsState()
        val pendingDeadlineLaunchId by deadlineNotificationLaunchTarget.pendingBandalartId.collectAsState()
        var deadlineNotificationAuthorizationStatus by remember {
            mutableStateOf(DeadlineNotificationAuthorizationStatus.UNSUPPORTED)
        }
        var deadlinePermissionRequestId by remember { mutableStateOf<Long?>(null) }
        var nextDeadlinePermissionRequestId by remember { mutableStateOf(0L) }
        val scope = rememberCoroutineScope()
        val recentEmojiSaveJobs = remember { mutableListOf<kotlinx.coroutines.Job>() }
        val selectionLoadGeneration = remember { longArrayOf(0L) }
        val handledCompletionRevision = remember { longArrayOf(-1L) }

        fun recordRecentEmoji(emoji: String) {
            val previousJob = recentEmojiSaveJobs.lastOrNull()
            recentEmojiSaveJobs.clear()
            recentEmojiSaveJobs +=
                scope.launch {
                    previousJob?.join()
                    settingsRepository.addRecentEmoji(emoji)
                }
        }

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

        suspend fun readBandalart(
            bandalartId: Long,
            isCompleted: Boolean = false,
        ): LoadedBandalart {
            val loadedBandalartData = bandalartRepository.getBandalart(bandalartId).toUiModel()

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

            val loadedBandalartCellData =
                BandalartCellEntity(
                    id = mainCell?.id ?: 0L,
                    title = mainCell?.title,
                    description = mainCell?.description,
                    dueDate = mainCell?.dueDate,
                    isCompleted = mainCell?.isCompleted ?: false,
                    parentId = mainCell?.parentId,
                    children = children,
                )
            return LoadedBandalart(
                bandalartData = loadedBandalartData,
                bandalartCellData = loadedBandalartCellData,
                isCompleted = isCompleted,
            )
        }

        suspend fun loadBandalart(
            bandalartId: Long,
            isCompleted: Boolean = false,
            canCommit: () -> Boolean = { true },
        ) {
            isLoading = true
            val refreshedBandalart = readBandalart(bandalartId, isCompleted)
            if (!canCommit()) return
            loadedBandalart = refreshedBandalart
            yield()
            isLoading = false
        }

        fun beginSelectionRequest(): Long = ++selectionLoadGeneration[0]

        suspend fun selectBandalart(
            requestGeneration: Long,
            bandalartId: Long,
            isCompleted: Boolean = false,
            persistRecent: Boolean = true,
        ): Boolean {
            if (selectionLoadGeneration[0] != requestGeneration) return false
            if (persistRecent) {
                bandalartRepository.setRecentBandalartId(bandalartId)
            }
            if (selectionLoadGeneration[0] != requestGeneration) return false
            loadBandalart(
                bandalartId = bandalartId,
                isCompleted = isCompleted,
                canCommit = { selectionLoadGeneration[0] == requestGeneration },
            )
            return selectionLoadGeneration[0] == requestGeneration
        }

        suspend fun createInitialBandalart() {
            if (isCreatingEmptyBandalart) return

            isCreatingEmptyBandalart = true
            val selectionRequest = beginSelectionRequest()
            isExplicitSelectionTargetPending = true
            val bandalart = bandalartRepository.createBandalart()
            Snapshot.withMutableSnapshot {
                if (bandalart != null && selectionLoadGeneration[0] == selectionRequest) {
                    explicitSelectionTargetId = bandalart.id
                }
                isExplicitSelectionTargetPending = false
            }
            bandalart?.let {
                bandalartRepository.upsertBandalartId(bandalart.id, bandalart.isCompleted)
            }
        }

        suspend fun createBandalart(templateId: BandalartTemplateId? = null): Boolean {
            val selectionRequest = beginSelectionRequest()
            isExplicitSelectionTargetPending = true
            val bandalart = bandalartRepository.createBandalart(templateId)
            Snapshot.withMutableSnapshot {
                if (bandalart != null && selectionLoadGeneration[0] == selectionRequest) {
                    explicitSelectionTargetId = bandalart.id
                }
                isExplicitSelectionTargetPending = false
            }
            bandalart ?: return false
            bottomSheet = null
            bandalartRepository.upsertBandalartId(bandalart.id, bandalart.isCompleted)
            emitEffect(HomeScreen.Effect.ShowCreateSnackbar)
            return true
        }

        suspend fun requestCreateBandalart(templateId: BandalartTemplateId? = null) {
            if (!rewardedRecoveryChecked) return
            if (!rewardedCreateCoordinator.beginSlotCheck()) return
            pendingCreationTemplateId = templateId
            val maxSlots =
                runRewardedOperation {
                    bandalartSlotRepository.getMaxBandalartSlots(bandalartList.size)
                }.getOrElse { exception ->
                    Napier.e("Failed to resolve bandalart slots", exception, tag = "RewardedAd")
                    rewardedCreateCoordinator.slotCheckFailed()
                    pendingCreationTemplateId = null
                    emitEffect(HomeScreen.Effect.ShowSlotErrorSnackbar)
                    return
                }
            if (
                rewardedCreateCoordinator.slotsResolved(
                    canCreate = bandalartList.size < maxSlots,
                    currentCount = bandalartList.size,
                )
            ) {
                var created = false
                try {
                    created = createBandalart(templateId)
                } finally {
                    rewardedCreateCoordinator.creationFinished(created, bandalartList.size)
                    pendingCreationTemplateId = null
                }
            } else {
                dialog = HomeScreen.DialogState.RewardedCreate
            }
        }

        fun confirmRewardedCreate() {
            val requestId = rewardedCreateCoordinator.confirm() ?: return

            dialog = null
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                withContext(NonCancellable) {
                    runRewardedOperation {
                        bandalartSlotRepository.prepareRewardedCreation(
                            requestId = requestId,
                            currentBandalartCount = bandalartList.size,
                            templateId = pendingCreationTemplateId,
                        )
                    }.onSuccess {
                        rewardedAdRequestId = requestId
                    }.onFailure { exception ->
                        Napier.e("Failed to persist rewarded request", exception, tag = "RewardedAd")
                        rewardedCreateCoordinator.adPreparationFailed(requestId)
                        pendingCreationTemplateId = null
                        emitEffect(HomeScreen.Effect.ShowSlotErrorSnackbar)
                    }
                }
            }
        }

        fun finishRewardedAd(
            requestId: Long,
            result: RewardedAdResult,
        ) {
            when (rewardedCreateCoordinator.adFinished(requestId, result)) {
                RewardedCompletion.IGNORED -> Unit
                RewardedCompletion.DISMISSED -> {
                    rewardedAdRequestId = null
                    pendingCreationTemplateId = null
                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                        withContext(NonCancellable) {
                            bandalartSlotRepository.clearPendingRewardedCreation(requestId)
                        }
                    }
                }
                RewardedCompletion.GRANTED -> {
                    rewardedAdRequestId = null
                    if (result == RewardedAdResult.FAILED) {
                        emitEffect(HomeScreen.Effect.ShowAdUnavailableSnackbar)
                    }
                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                        var created = false
                        var expectedCount = bandalartList.size + 1
                        try {
                            withContext(NonCancellable) {
                                val pending =
                                    runRewardedOperation {
                                        bandalartSlotRepository.grantRewardedCreation(requestId)
                                    }.onFailure { exception ->
                                        Napier.e("Failed to persist rewarded bandalart slot", exception, tag = "RewardedAd")
                                        emitEffect(HomeScreen.Effect.ShowSlotErrorSnackbar)
                                    }.getOrNull()
                                if (pending != null) {
                                    expectedCount = pending.targetSlots
                                    created =
                                        runRewardedOperation { createBandalart(pending.templateId) }
                                            .onFailure { exception ->
                                                Napier.e("Failed to create rewarded bandalart", exception, tag = "RewardedAd")
                                            }.getOrDefault(false)
                                }
                            }
                        } finally {
                            pendingCreationTemplateId = null
                            rewardedCreateCoordinator.grantFinished(
                                wasCreated = created,
                                expectedCount = expectedCount,
                                currentCount = bandalartList.size,
                            )
                        }
                    }
                }
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

        fun updateEmojiDraft(emoji: String): Boolean {
            val currentSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell ?: return false
            bottomSheet =
                currentSheet.copy(
                    bandalartData = currentSheet.bandalartData.copy(profileEmoji = emoji),
                    isEmojiPickerOpened = false,
                )
            return true
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

        suspend fun toggleTaskCompletion(requestedCell: BandalartCellEntity) {
            val currentBandalart = bandalartData ?: return
            val currentCell = bandalartCellData?.findTaskCell(requestedCell.id) ?: return
            if (currentCell.title.isNullOrBlank()) return
            if (!togglingTaskCellIds.add(currentCell.id)) return
            val updatedCompletion = !currentCell.isCompleted

            try {
                runCatching {
                    bandalartRepository.updateBandalartTaskCell(
                        bandalartId = currentBandalart.id,
                        cellId = currentCell.id,
                        updateBandalartTaskCellEntity =
                            UpdateBandalartTaskCellEntity(
                                title = currentCell.title,
                                description = currentCell.description,
                                dueDate = currentCell.dueDate,
                                isCompleted = updatedCompletion,
                            ),
                    )
                    val activeBandalart = loadedBandalart
                    if (activeBandalart?.bandalartData?.id != currentBandalart.id) {
                        return@runCatching null
                    }
                    readBandalart(
                        bandalartId = currentBandalart.id,
                        isCompleted = activeBandalart.isCompleted,
                    )
                }.onSuccess { refreshedBandalart ->
                    if (
                        refreshedBandalart != null &&
                        loadedBandalart?.bandalartData?.id == currentBandalart.id
                    ) {
                        val refreshedCell = refreshedBandalart.bandalartCellData.findTaskCell(currentCell.id)
                        loadedBandalart = refreshedBandalart
                        if (
                            refreshedCell != null &&
                            !refreshedCell.title.isNullOrBlank() &&
                            refreshedCell.isCompleted == updatedCompletion
                        ) {
                            emitEffect(HomeScreen.Effect.PlayTaskCompletionHaptic(currentCell.id))
                        }
                    }
                }.onFailure { exception ->
                    if (exception is CancellationException) throw exception
                    Napier.e("Failed to toggle task cell completion", exception, tag = "HomePresenter")
                }
            } finally {
                togglingTaskCellIds.remove(currentCell.id)
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
                val previousList = bandalartRepository.getPrevBandalartList()
                val completedTargetId =
                    currentList
                        .filter { bandalart ->
                            val previous = previousList.find { it.first == bandalart.id }
                            previous != null && !previous.second && bandalart.isCompleted
                        }.firstOrNull()
                        ?.id
                val pendingRewardedCreation =
                    runRewardedOperation { bandalartSlotRepository.getPendingRewardedCreation() }
                        .onFailure { exception ->
                            Napier.e("Failed to recover rewarded creation", exception, tag = "RewardedAd")
                        }.getOrNull()
                if (pendingRewardedCreation?.isGranted == true) {
                    if (currentList.size >= pendingRewardedCreation.targetSlots) {
                        runRewardedOperation {
                            bandalartSlotRepository.clearPendingRewardedCreation(
                                pendingRewardedCreation.requestId,
                            )
                        }.onFailure { exception ->
                            Napier.e("Failed to clear rewarded recovery", exception, tag = "RewardedAd")
                        }
                        rewardedCreateCoordinator.creationObserved(currentList.size)
                    } else if (
                        rewardedCreateCoordinator.beginPendingRecovery(
                            pendingRewardedCreation.targetSlots,
                        )
                    ) {
                        var created = false
                        try {
                            created =
                                runRewardedOperation { createBandalart(pendingRewardedCreation.templateId) }
                                    .onFailure { exception ->
                                        Napier.e("Failed to recover rewarded bandalart", exception, tag = "RewardedAd")
                                    }.getOrDefault(false)
                        } finally {
                            rewardedCreateCoordinator.grantFinished(
                                wasCreated = created,
                                expectedCount = pendingRewardedCreation.targetSlots,
                                currentCount = currentList.size,
                            )
                        }
                    }
                } else {
                    rewardedCreateCoordinator.creationObserved(currentList.size)
                }
                rewardedRecoveryChecked = true

                if (completedTargetId == null) {
                    currentList.forEach { bandalart ->
                        bandalartRepository.upsertBandalartId(
                            bandalartId = bandalart.id,
                            isCompleted = bandalart.isCompleted,
                        )
                    }
                }

                if (currentList.isEmpty()) {
                    createInitialBandalart()
                    return@collect
                }

                isCreatingEmptyBandalart = false
                Snapshot.withMutableSnapshot {
                    newlyCompletedTargetId = completedTargetId
                    bandalartList = currentList.toPersistentList()
                    bandalartListRevision += 1
                }
            }
        }

        LaunchedEffect(
            pendingDeadlineLaunchId,
            isExplicitSelectionTargetPending,
            explicitSelectionTargetId,
            bandalartListRevision,
        ) {
            if (bandalartList.isEmpty()) return@LaunchedEffect
            val targetId = pendingDeadlineLaunchId
            if (targetId != null) {
                val selectionRequest = beginSelectionRequest()
                explicitSelectionTargetId = null
                isExplicitSelectionTargetPending = false
                if (bandalartList.none { it.id == targetId }) {
                    deadlineNotificationLaunchTarget.acknowledge(targetId)
                    return@LaunchedEffect
                }
                val committed = selectBandalart(selectionRequest, targetId)
                if (!committed) return@LaunchedEffect
                handledCompletionRevision[0] = bandalartListRevision
                bottomSheet = null
                deadlineNotificationLaunchTarget.acknowledge(targetId)
                return@LaunchedEffect
            }
            if (isExplicitSelectionTargetPending) return@LaunchedEffect
            val explicitTargetId = explicitSelectionTargetId
            if (explicitTargetId != null) {
                val selectionRequest = beginSelectionRequest()
                val committed = selectBandalart(selectionRequest, explicitTargetId)
                if (committed) {
                    explicitSelectionTargetId = null
                    bottomSheet = null
                }
                return@LaunchedEffect
            }
            val selectionRequest = beginSelectionRequest()
            val completedTargetId =
                newlyCompletedTargetId?.takeIf { completedId ->
                    handledCompletionRevision[0] != bandalartListRevision &&
                        bandalartList.any { it.id == completedId }
                }
            val currentBandalartId = bandalartData?.id
            val currentSelectionId =
                currentBandalartId?.takeIf { currentId -> bandalartList.any { it.id == currentId } }
            val recentBandalartId = bandalartRepository.getRecentBandalartId()
            val selectedId =
                completedTargetId ?: currentSelectionId ?: recentBandalartId.takeIf { recentId ->
                    bandalartList.any { it.id == recentId }
                } ?: bandalartList.first().id
            selectBandalart(
                requestGeneration = selectionRequest,
                bandalartId = selectedId,
                isCompleted = selectedId == completedTargetId,
                persistRecent = selectedId != currentSelectionId,
            )
            if (selectedId == completedTargetId) {
                handledCompletionRevision[0] = bandalartListRevision
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
            recentEmojis = recentEmojis.toPersistentList(),
            rewardedAdRequestId = rewardedAdRequestId,
            deadlineReminderEnabled = deadlineReminderEnabled,
            deadlineNotificationAuthorizationStatus = deadlineNotificationAuthorizationStatus,
            deadlineReminderSchedulingHealth = deadlineReminderSchedulingHealth,
            deadlinePermissionRequestId = deadlinePermissionRequestId,
            effect = effect,
        ) { event ->
            when (event) {
                is HomeScreen.Event.SelectBandalart -> {
                    beginSelectionRequest()
                    explicitSelectionTargetId = event.bandalartId
                }

                HomeScreen.Event.AddBandalart -> scope.launch { requestCreateBandalart() }
                HomeScreen.Event.OpenBandalartCreationOptions -> {
                    val currentSheet = bottomSheet as? HomeScreen.BottomSheetState.BandalartList
                    currentSheet?.let { bottomSheet = it.copy(isCreationOptionsVisible = true) }
                }
                HomeScreen.Event.CloseBandalartCreationOptions -> {
                    val currentSheet = bottomSheet as? HomeScreen.BottomSheetState.BandalartList
                    currentSheet?.let { bottomSheet = it.copy(isCreationOptionsVisible = false) }
                }
                is HomeScreen.Event.CreateBandalartFromTemplate -> {
                    scope.launch { requestCreateBandalart(event.templateId) }
                }
                HomeScreen.Event.ConfirmRewardedCreate -> confirmRewardedCreate()
                is HomeScreen.Event.RewardedAdFinished -> {
                    finishRewardedAd(event.requestId, event.result)
                }
                HomeScreen.Event.OpenBandalartList -> openBandalartList()
                HomeScreen.Event.OpenSettings -> {
                    bottomSheet = HomeScreen.BottomSheetState.Settings
                    scope.launch {
                        deadlineNotificationAuthorizationStatus = deadlineNotificationAuthorization.getStatus()
                    }
                }
                HomeScreen.Event.OpenEmoji -> {
                    if (!isUpdatingBandalartEmoji) openEmoji()
                }
                is HomeScreen.Event.OpenCell ->
                    openCell(
                        cellType = event.cellType,
                        isMainCellTitleEmpty = event.isMainCellTitleEmpty,
                        cellData = event.cellData,
                    )

                is HomeScreen.Event.ToggleTaskCompletion -> {
                    scope.launch { toggleTaskCompletion(event.cellData) }
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
                HomeScreen.Event.DismissDialog -> {
                    if (dialog == HomeScreen.DialogState.RewardedCreate) {
                        rewardedCreateCoordinator.dismissDialog()
                        pendingCreationTemplateId = null
                    }
                    dialog = null
                }
                is HomeScreen.Event.UpdateCellTitle ->
                    updateCellTitle(
                        title = event.title,
                        language = event.language,
                    )

                is HomeScreen.Event.UpdateDescription -> updateDescription(event.description)
                is HomeScreen.Event.UpdateDueDate -> updateDueDate(event.dueDate)
                is HomeScreen.Event.UpdateCompletion -> updateCompletion(event.isCompleted)
                is HomeScreen.Event.UpdateEmojiDraft -> {
                    if (updateEmojiDraft(event.emoji)) {
                        recordRecentEmoji(event.emoji)
                    }
                }
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

                HomeScreen.Event.CloseEmojiPicker -> {
                    val currentSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell
                    currentSheet?.let { bottomSheet = it.copy(isEmojiPickerOpened = false) }
                }

                HomeScreen.Event.SaveCell -> scope.launch { saveCell() }
                is HomeScreen.Event.UpdateBandalartEmoji -> {
                    if (
                        !isUpdatingBandalartEmoji &&
                        bottomSheet is HomeScreen.BottomSheetState.Emoji
                    ) {
                        isUpdatingBandalartEmoji = true
                        bottomSheet = null
                        event.emoji?.let(::recordRecentEmoji)
                        scope.launch {
                            try {
                                updateBandalartEmoji(
                                    bandalartId = event.bandalartId,
                                    cellId = event.cellId,
                                    emoji = event.emoji,
                                )
                            } finally {
                                isUpdatingBandalartEmoji = false
                            }
                        }
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

                is HomeScreen.Event.SetDeadlineReminderEnabled -> {
                    if (!event.enabled) {
                        scope.launch {
                            settingsRepository.setDeadlineReminderEnabled(false)
                            deadlineReminderReconciler.reconcileAll()
                        }
                    }
                }

                HomeScreen.Event.ConfirmDeadlineReminderPermission -> {
                    scope.launch {
                        deadlineNotificationAuthorizationStatus = deadlineNotificationAuthorization.getStatus()
                        when (deadlineNotificationAuthorizationStatus) {
                            DeadlineNotificationAuthorizationStatus.GRANTED,
                            DeadlineNotificationAuthorizationStatus.QUIET,
                            -> {
                                settingsRepository.setDeadlineReminderEnabled(true)
                                deadlineReminderReconciler.reconcileAll()
                            }

                            DeadlineNotificationAuthorizationStatus.REQUESTABLE -> {
                                deadlineNotificationAuthorizationStatus =
                                    deadlineNotificationAuthorization.requestAuthorization()
                                when (deadlineNotificationAuthorizationStatus) {
                                    DeadlineNotificationAuthorizationStatus.GRANTED,
                                    DeadlineNotificationAuthorizationStatus.QUIET,
                                    -> {
                                        settingsRepository.setDeadlineReminderEnabled(true)
                                        deadlineReminderReconciler.reconcileAll()
                                    }

                                    DeadlineNotificationAuthorizationStatus.REQUESTABLE -> {
                                        nextDeadlinePermissionRequestId += 1
                                        deadlinePermissionRequestId = nextDeadlinePermissionRequestId
                                    }

                                    DeadlineNotificationAuthorizationStatus.BLOCKED -> {
                                        settingsRepository.setDeadlineReminderEnabled(false)
                                        deadlineReminderReconciler.reconcileAll()
                                    }

                                    DeadlineNotificationAuthorizationStatus.UNSUPPORTED -> Unit
                                }
                            }

                            DeadlineNotificationAuthorizationStatus.BLOCKED -> {
                                deadlineNotificationAuthorization.openSettings()
                            }

                            DeadlineNotificationAuthorizationStatus.UNSUPPORTED -> Unit
                        }
                    }
                }

                HomeScreen.Event.DeadlineReminderPermissionResult -> {
                    deadlinePermissionRequestId = null
                    scope.launch {
                        deadlineNotificationAuthorizationStatus = deadlineNotificationAuthorization.getStatus()
                        val granted =
                            deadlineNotificationAuthorizationStatus == DeadlineNotificationAuthorizationStatus.GRANTED ||
                                deadlineNotificationAuthorizationStatus == DeadlineNotificationAuthorizationStatus.QUIET
                        settingsRepository.setDeadlineReminderEnabled(granted)
                        deadlineReminderReconciler.reconcileAll()
                    }
                }

                HomeScreen.Event.DeadlineReminderForegrounded -> {
                    scope.launch {
                        deadlineNotificationAuthorizationStatus = deadlineNotificationAuthorization.getStatus()
                        if (deadlineReminderEnabled) {
                            deadlineReminderReconciler.reconcileAll()
                        }
                    }
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
                        loadedBandalart = loadedBandalart?.copy(isCompleted = false)
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
        const val MAX_DESCRIPTION_LENGTH = 1000
    }
}

private data class LoadedBandalart(
    val bandalartData: BandalartUiModel,
    val bandalartCellData: BandalartCellEntity,
    val isCompleted: Boolean,
)

@Suppress("TooGenericExceptionCaught")
private inline fun <T> runRewardedOperation(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception)
    }

private fun BandalartCellEntity.findTaskCell(cellId: Long): BandalartCellEntity? =
    children
        .asSequence()
        .flatMap { subCell -> subCell.children.asSequence() }
        .firstOrNull { taskCell -> taskCell.id == cellId }
