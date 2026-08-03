package com.nexters.bandalart.feature.home.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.nexters.bandalart.core.common.utils.UiText
import com.nexters.bandalart.core.common.utils.isValidImmediateAppUpdate
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartEmojiEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartMainCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartSubCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartTaskCellEntity
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import com.nexters.bandalart.core.domain.repository.InAppUpdateRepository
import com.nexters.bandalart.core.ui.R
import com.nexters.bandalart.feature.complete.CompleteScreen
import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.home.HomeScreen.Event
import com.nexters.bandalart.feature.home.HomeScreen.State
import com.nexters.bandalart.feature.home.mapper.toUiModel
import com.nexters.bandalart.feature.home.model.BandalartUiModel
import com.nexters.bandalart.feature.home.model.CellType
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.internal.rememberStableCoroutineScope
import com.slack.circuit.runtime.presenter.Presenter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.components.ActivityRetainedComponent
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

class HomePresenter @AssistedInject constructor(
    @Assisted private val navigator: Navigator,
    private val bandalartRepository: BandalartRepository,
    private val inAppUpdateRepository: InAppUpdateRepository,
) : Presenter<State> {
    @Composable
    override fun present(): State {
        var bandalartList by rememberRetained { mutableStateOf(persistentListOf<BandalartUiModel>()) }
        var bandalartData by rememberRetained { mutableStateOf<BandalartUiModel?>(null) }
        var bandalartCellData by rememberRetained { mutableStateOf<BandalartCellEntity?>(null) }
        var bandalartChartUrl by rememberRetained { mutableStateOf("") }
        var isBandalartCompleted by rememberRetained { mutableStateOf(false) }
        var bottomSheet by rememberSaveable { mutableStateOf<HomeScreen.BottomSheetState?>(null) }
        var dialog by rememberSaveable { mutableStateOf<HomeScreen.DialogState?>(null) }
        var isDropDownMenuOpened by rememberRetained { mutableStateOf(false) }
        var isSharing by rememberRetained { mutableStateOf(false) }
        var isCapturing by rememberRetained { mutableStateOf(false) }
        var clickedCellType by rememberRetained { mutableStateOf(CellType.MAIN) }
        var clickedCellData by rememberRetained { mutableStateOf(BandalartCellEntity()) }
        var updateVersionCode by rememberRetained { mutableStateOf<Int?>(null) }
        var sideEffect by rememberRetained { mutableStateOf<HomeScreen.SideEffect?>(null) }

        val scope = rememberStableCoroutineScope()

        fun requestCapture() {
            isCapturing = true
        }

        fun requestShare() {
            isSharing = true
        }

        fun clearShareState() {
            isSharing = false
        }

        fun clearCaptureState() {
            isCapturing = false
        }

        fun clearSideEffect() {
            sideEffect = null
        }

        suspend fun getBandalartMainCell(bandalartId: Long) {
            val mainCell = bandalartRepository.getBandalartMainCell(bandalartId)
            val subCells = bandalartRepository.getChildCells(mainCell?.id ?: 0L)

            val children = subCells.map { subCell ->
                val taskCells = bandalartRepository.getChildCells(subCell.id)
                BandalartCellEntity(
                    id = subCell.id,
                    title = subCell.title,
                    description = subCell.description,
                    dueDate = subCell.dueDate,
                    isCompleted = subCell.isCompleted,
                    parentId = subCell.parentId,
                    children = taskCells.map { taskCell ->
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

            bandalartCellData = BandalartCellEntity(
                id = mainCell?.id ?: 0L,
                title = mainCell?.title,
                description = mainCell?.description,
                dueDate = mainCell?.dueDate,
                isCompleted = mainCell?.isCompleted ?: false,
                parentId = mainCell?.parentId,
                children = children,
            )
        }

        suspend fun getBandalart(
            bandalartId: Long,
            isCompleted: Boolean = false,
        ) {
            bandalartRepository.getBandalart(bandalartId).let { bandalart ->
                bandalartData = bandalart.toUiModel()
                bottomSheet = null
                isBandalartCompleted = isCompleted
                getBandalartMainCell(bandalartId)
            }
        }

        fun showSnackbar(message: UiText) {
            sideEffect = HomeScreen.SideEffect.ShowSnackbar(message = message)
        }

        fun showToast(message: UiText) {
            sideEffect = HomeScreen.SideEffect.ShowToast(message = message)
        }

        suspend fun createBandalart() {
            if ((bandalartList.size + 1) > 5) {
                showToast(UiText.StringResource(R.string.limit_create_bandalart))
                return
            }

            bandalartRepository.createBandalart()?.let { bandalart ->
                bottomSheet = null
                getBandalart(bandalart.id)
                setRecentBandalartId(bandalart.id)
                upsertBandalartId(bandalart.id, bandalart.isCompleted)
                showSnackbar(UiText.StringResource(R.string.create_bandalart))
            }
        }

        fun showBandalartListBottomSheet() {
            bottomSheet = HomeScreen.BottomSheetState.BandalartList(
                bandalartList = bandalartList,
                currentBandalartId = bandalartData?.id ?: return,
            )
        }

        fun showEmojiBottomSheet() {
            bandalartData?.let { bandalartData ->
                bottomSheet = HomeScreen.BottomSheetState.Emoji(
                    bandalartId = bandalartData.id,
                    cellId = bandalartData.id,
                    currentEmoji = bandalartData.profileEmoji,
                )
            }
        }

        fun showBandalartDeleteDialog() {
            dialog = HomeScreen.DialogState.BandalartDelete
        }

        fun showDropDownMenu() {
            isDropDownMenuOpened = true
        }

        fun showAppVersion() {
            sideEffect = HomeScreen.SideEffect.ShowAppVersion
        }

        fun expandEmojiPicker() {
            val currentBottomSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell ?: return
            bottomSheet = currentBottomSheet.copy(isEmojiPickerOpened = true)
        }

        fun shrinkEmojiPicker() {
            val currentBottomSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell ?: return
            bottomSheet = currentBottomSheet.copy(isEmojiPickerOpened = false)
        }

        fun expandDatePicker() {
            val currentSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell ?: return
            bottomSheet = currentSheet.copy(isDatePickerOpened = true)
        }

        fun shrinkDatePicker() {
            val currentSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell ?: return
            bottomSheet = currentSheet.copy(isDatePickerOpened = false)
        }

        fun hideDropDownMenu() {
            isDropDownMenuOpened = false
        }

        fun hideDialog() {
            dialog = null
        }

        fun hideBottomSheet() {
            bottomSheet = null
        }

        fun hideModal() {
            hideDialog()
            hideBottomSheet()
        }

        fun updateCellTitle(title: String, locale: Locale) {
            val currentBottomSheet =
                bottomSheet as? HomeScreen.BottomSheetState.Cell ?: return
            val maxLength = when (locale.language) {
                Locale.KOREAN.language, Locale.JAPAN.language -> 15
                else -> 24
            }

            val validatedTitle = if (title.length > maxLength) {
                currentBottomSheet.cellData.title ?: ""
            } else title

            bottomSheet = currentBottomSheet.copy(
                cellData = currentBottomSheet.cellData.copy(title = validatedTitle),
            )
        }

        fun updateDescription(description: String) {
            val currentBottomSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell ?: return
            val validatedDescription = if (description.length > 1000) {
                currentBottomSheet.cellData.description
            } else {
                description
            }
            bottomSheet = currentBottomSheet.copy(
                cellData = currentBottomSheet.cellData.copy(description = validatedDescription),
            )
        }

        fun updateDueDate(date: String) {
            val currentBottomSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell ?: return
            bottomSheet = currentBottomSheet.copy(
                cellData = currentBottomSheet.cellData.copy(dueDate = date),
            )
            shrinkDatePicker()
        }

        fun updateThemeColor(mainColor: String, subColor: String) {
            val currentBottomSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell ?: return
            bottomSheet = currentBottomSheet.copy(
                bandalartData = currentBottomSheet.bandalartData.copy(
                    mainColor = mainColor,
                    subColor = subColor,
                ),
            )
            bandalartData = bandalartData?.copy(mainColor = mainColor, subColor = subColor)
        }

        fun updateEmoji(emoji: String) {
            val currentBottomSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell ?: return
            bottomSheet = currentBottomSheet.copy(
                bandalartData = currentBottomSheet.bandalartData.copy(profileEmoji = emoji),
            )
            shrinkEmojiPicker()
        }

        fun updateCompletion(isCompleted: Boolean) {
            val currentBottomSheet = bottomSheet as? HomeScreen.BottomSheetState.Cell ?: return
            bottomSheet = currentBottomSheet.copy(
                cellData = currentBottomSheet.cellData.copy(isCompleted = isCompleted),
            )
        }

        suspend fun updateCell(
            bandalartId: Long,
            cellId: Long,
            cellType: CellType,
        ) {
            val bottomSheetData =
                bottomSheet as? HomeScreen.BottomSheetState.Cell ?: return
            val cellData = bottomSheetData.cellData

            val trimmedTitle = cellData.title?.trim()
            val description = cellData.description
            val dueDate = cellData.dueDate?.ifEmpty { null }

            when (cellType) {
                CellType.MAIN -> {
                    updateMainCell(
                        bandalartId = bandalartId,
                        cellId = cellId,
                        updateBandalartMainCellModel = UpdateBandalartMainCellEntity(
                            title = trimmedTitle,
                            description = description,
                            dueDate = dueDate,
                            profileEmoji = bottomSheetData.bandalartData.profileEmoji,
                            mainColor = bottomSheetData.bandalartData.mainColor,
                            subColor = bottomSheetData.bandalartData.subColor,
                        ),
                    )
                }

                CellType.SUB -> {
                    updateSubCell(
                        bandalartId = bandalartId,
                        cellId = cellId,
                        updateBandalartSubCellModel = UpdateBandalartSubCellEntity(
                            title = trimmedTitle,
                            description = description,
                            dueDate = dueDate,
                        ),
                    )
                }

                else -> {
                    updateTaskCell(
                        bandalartId = bandalartId,
                        cellId = cellId,
                        updateBandalartTaskCellModel = UpdateBandalartTaskCellEntity(
                            title = trimmedTitle,
                            description = description,
                            dueDate = dueDate,
                            isCompleted = cellData.isCompleted,
                        ),
                    )
                }
            }
        }

        fun handleBandalartCellClick(
            cellType: CellType,
            isMainCellTitleEmpty: Boolean,
            cellData: BandalartCellEntity,
        ) {
            when {
                cellType != CellType.MAIN && isMainCellTitleEmpty -> {
                    showToast(UiText.StringResource(R.string.please_input_main_goal))
                }

                else -> {
                    clickedCellData = cellData
                    clickedCellType = cellType
                    bottomSheet = bandalartData?.let { bandalartData ->
                        HomeScreen.BottomSheetState.Cell(
                            initialCellData = cellData,
                            cellData = cellData,
                            initialBandalartData = bandalartData,
                            bandalartData = bandalartData,
                        )
                    }
                }
            }
        }

        fun shareBandalart(bitmap: ImageBitmap) {
            clearShareState()
            sideEffect = HomeScreen.SideEffect.ShareImage(bitmap)
//            context.bitmapToFileUri(bitmap)?.let { uri ->
//                navigator.goTo(ShareScreen(uri.toString()))
//            }
        }

        fun saveBandalartImage(bitmap: ImageBitmap) {
            clearCaptureState()
            sideEffect = HomeScreen.SideEffect.SaveImage(bitmap)
        }

        fun captureBandalart(bitmap: ImageBitmap) {
            clearCaptureState()
            sideEffect = HomeScreen.SideEffect.CaptureImage(bitmap)
//            context.bitmapToFileUri(bitmap)?.let { uri ->
//                bandalartChartUrl = uri.toString()
//            }
        }

        // 반다라트 완료 상태 관찰
        LaunchedEffect(bandalartData) {
            bandalartData?.let { bandalart ->
                if (bandalart.isCompleted && bandalart.title?.isNotEmpty() == true) {
                    val isCompleted = checkCompletedBandalartId(bandalart.id)
                    if (isCompleted) {
                        delay(500L)
                        requestCapture()
                        delay(500L)
                        navigator.goTo(
                            CompleteScreen(
                                bandalartId = bandalart.id,
                                bandalartTitle = bandalart.title,
                                bandalartProfileEmoji = bandalart.profileEmoji.orEmpty(),
                                bandalartChartImageUri = bandalartChartUrl,
                            ),
                        )
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            bandalartRepository.getBandalartList()
                .map { list -> list.map { it.toUiModel() } }
                .collect { list ->
                    bandalartList = list.toPersistentList()

                    // 이전 반다라트 목록 상태 조회
                    val prevBandalartList = bandalartRepository.getPrevBandalartList()

                    // 새로 업데이트 된 상태와 이전 상태를 비교
                    val completedKeys = list.filter { bandalart ->
                        val prevBandalart = prevBandalartList.find { it.first == bandalart.id }
                        prevBandalart != null && !prevBandalart.second && bandalart.isCompleted
                    }.map { it.id }

                    // 이번에 목표를 달성한 반다라트가 존재하는 경우
                    if (completedKeys.isNotEmpty()) {
                        getBandalart(completedKeys[0], true)
                        return@collect
                    }

                    // 데이터를 동기화
                    list.forEach { bandalart ->
                        bandalartRepository.upsertBandalartId(bandalart.id, bandalart.isCompleted)
                    }

                    // 반다라트 목록이 존재하지 않을 경우, 새로운 반다라트를 생성
                    if (list.isEmpty()) {
                        createBandalart()
                    } else {
                        // 가장 최근에 확인한 반다라트 표를 화면에 띄우는 경우
                        val recentBandalartId = getRecentBandalartId()
                        // 가장 최근에 확인한 반다라트 표가 존재하는 경우
                        if (list.any { it.id == recentBandalartId }) {
                            getBandalart(recentBandalartId)
                        } else {
                            // 목록에 가장 첫번째 표를 화면에 띄움
                            getBandalart(list[0].id)
                        }
                    }
                }
        }

        fun handleEvent(event: Event) {
            when (event) {
                is Event.OnListClick -> showBandalartListBottomSheet()
                is Event.OnSaveClick -> requestCapture()
                is Event.OnDeleteClick -> showBandalartDeleteDialog()
                is Event.OnEmojiSelected -> {
                    scope.launch {
                        updateBandalartEmoji(
                            event.bandalartId,
                            event.cellId,
                            event.updateBandalartEmojiModel,
                        )
                        hideBottomSheet()
                    }
                }

                is Event.OnShareButtonClick -> requestShare()
                is Event.OnAddClick -> {
                    scope.launch {
                        createBandalart()
                    }
                }

                is Event.OnAppTitleClick -> showAppVersion()
                // TODO 태스크셀이고 상위 서브셀이 비어있을 때(테스트셀이 자신의 부모를 알고있어야 구현 가능 함)
                is Event.OnBandalartCellClick -> handleBandalartCellClick(
                    event.cellType,
                    event.isMainCellTitleEmpty,
                    event.cellData,
                )

                is Event.OnBandalartListItemClick -> {
                    scope.launch {
                        setRecentBandalartId(event.key)
                        getBandalart(event.key)
                        hideBottomSheet()
                    }
                }

                is Event.OnCancelClick -> hideDialog()
                is Event.OnCellTitleUpdate -> updateCellTitle(event.title, event.locale)
                is Event.OnCloseButtonClick -> hideBottomSheet()
                is Event.OnColorSelect -> updateThemeColor(event.mainColor, event.subColor)
                is Event.OnCompleteButtonClick -> {
                    scope.launch {
                        updateCell(event.bandalartId, event.cellId, event.cellType)
                    }
                }

                is Event.OnCompletionUpdate -> updateCompletion(event.isCompleted)
                is Event.OnDatePickerClick -> expandDatePicker()
                is Event.OnDeleteBandalart -> {
                    scope.launch {
                        deleteBandalart(event.bandalartId)
                        hideModal()
                        hideDropDownMenu()
                        showSnackbar(UiText.StringResource(R.string.delete_bandalart))
                    }
                }

                is Event.OnDeleteButtonClick -> {
                    dialog = HomeScreen.DialogState.CellDelete(
                        cellType = clickedCellType,
                        cellTitle = (bottomSheet as? HomeScreen.BottomSheetState.Cell)?.cellData?.title,
                    )
                }

                is Event.OnDeleteCell -> {
                    scope.launch {
                        deleteCell(event.cellId)
                        hideModal()
                    }
                }

                is Event.OnDescriptionUpdate -> updateDescription(event.description)
                is Event.OnDismiss -> hideBottomSheet()
                is Event.OnDropDownMenuDismiss -> hideDropDownMenu()
                is Event.OnDueDateSelect -> updateDueDate(event.date)
                is Event.OnEmojiClick -> showEmojiBottomSheet()
                is Event.OnEmojiPickerClick -> expandEmojiPicker()
                is Event.OnEmojiSelect -> updateEmoji(event.emoji)
                is Event.OnMenuClick -> showDropDownMenu()
                is Event.OnShareRequested -> shareBandalart(event.bitmap)
                is Event.OnSaveRequested -> saveBandalartImage(event.bitmap)
                is Event.OnCaptureRequested -> captureBandalart(event.bitmap)
                is Event.OnUpdateCheck -> {
                    scope.launch {
                        if (!isValidImmediateAppUpdate(event.versionCode) &&
                            !isUpdateAlreadyRejected(event.versionCode)
                        ) {
                            updateVersionCode = event.versionCode
                        }
                    }
                }

                is Event.OnUpdateCanceled -> {
                    scope.launch {
                        updateVersionCode?.let {
                            setLastRejectedUpdateVersion(it)
                        }
                        updateVersionCode = null
                    }
                }

                is Event.CaptureFinished -> {
                    bandalartChartUrl = event.bandalartChartUrl
                }

                is Event.InitSideEffect -> {
                    clearSideEffect()
                }
            }
        }

        return State(
            bandalartList = bandalartList,
            bandalartData = bandalartData,
            bandalartCellData = bandalartCellData,
            bandalartChartUrl = bandalartChartUrl,
            isBandalartCompleted = isBandalartCompleted,
            bottomSheet = bottomSheet,
            dialog = dialog,
            isSharing = isSharing,
            isCapturing = isCapturing,
            isDropDownMenuOpened = isDropDownMenuOpened,
            clickedCellType = clickedCellType,
            clickedCellData = clickedCellData,
            updateVersionCode = updateVersionCode,
            sideEffect = sideEffect,
            eventSink = { event -> handleEvent(event) },
        )
    }

    private suspend fun updateMainCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartMainCellModel: UpdateBandalartMainCellEntity,
    ) {
        bandalartRepository.updateBandalartMainCell(
            bandalartId,
            cellId,
            updateBandalartMainCellModel,
        )
        bandalartRepository.getBandalart(bandalartId)
    }

    private suspend fun updateSubCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartSubCellModel: UpdateBandalartSubCellEntity,
    ) {
        bandalartRepository.updateBandalartSubCell(bandalartId, cellId, updateBandalartSubCellModel)
    }

    private suspend fun updateTaskCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartTaskCellModel: UpdateBandalartTaskCellEntity,
    ) {
        bandalartRepository.updateBandalartTaskCell(
            bandalartId,
            cellId,
            updateBandalartTaskCellModel,
        )
    }

    private suspend fun updateBandalartEmoji(
        bandalartId: Long,
        cellId: Long,
        updateBandalartEmojiModel: UpdateBandalartEmojiEntity,
    ) {
        bandalartRepository.updateBandalartEmoji(bandalartId, cellId, updateBandalartEmojiModel)
    }

    private suspend fun getRecentBandalartId(): Long {
        return bandalartRepository.getRecentBandalartId()
    }

    private suspend fun setRecentBandalartId(bandalartId: Long) {
        bandalartRepository.setRecentBandalartId(bandalartId)
    }

    private suspend fun upsertBandalartId(
        bandalartId: Long,
        isCompleted: Boolean = false,
    ) {
        bandalartRepository.upsertBandalartId(bandalartId, isCompleted)
    }

    private suspend fun checkCompletedBandalartId(bandalartId: Long): Boolean {
        return bandalartRepository.checkCompletedBandalartId(bandalartId)
    }

    private suspend fun deleteBandalartId(bandalartId: Long) {
        bandalartRepository.deleteCompletedBandalartId(bandalartId)
    }

    private suspend fun deleteCell(cellId: Long) {
        bandalartRepository.deleteBandalartCell(cellId)
    }

    private suspend fun deleteBandalart(bandalartId: Long) {
        bandalartRepository.deleteBandalart(bandalartId)
        deleteBandalartId(bandalartId)
    }

    private suspend fun setLastRejectedUpdateVersion(versionCode: Int) {
        inAppUpdateRepository.setLastRejectedUpdateVersion(versionCode)
    }

    private suspend fun isUpdateAlreadyRejected(versionCode: Int): Boolean {
        return inAppUpdateRepository.isUpdateAlreadyRejected(versionCode)
    }

    @CircuitInject(HomeScreen::class, ActivityRetainedComponent::class)
    @AssistedFactory
    fun interface Factory {
        fun create(navigator: Navigator): HomePresenter
    }
}
