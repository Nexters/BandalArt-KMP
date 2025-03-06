package com.nexters.bandalart.feature.home.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.create_bandalart
import bandalart.core.designsystem.generated.resources.delete_bandalart
import bandalart.core.designsystem.generated.resources.limit_create_bandalart
import bandalart.core.designsystem.generated.resources.please_input_main_goal
import com.nexters.bandalart.core.common.Language
import com.nexters.bandalart.core.common.Locale
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartEmojiEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartMainCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartSubCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartTaskCellEntity
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import com.nexters.bandalart.core.domain.repository.InAppUpdateRepository
import com.nexters.bandalart.feature.home.mapper.toUiModel
import com.nexters.bandalart.feature.home.model.CellType
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

class HomeViewModel(
    private val bandalartRepository: BandalartRepository,
    private val inAppUpdateRepository: InAppUpdateRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<HomeUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val bandalartFlow = uiState
        .map { it.bandalartData }
        .filterNotNull()
        .distinctUntilChanged()

    init {
        updateSkeletonState(true)
        getBandalartList()
        observeBandalartCompletion()
    }

    private fun observeBandalartCompletion() {
        viewModelScope.launch {
            bandalartFlow.collect { bandalart ->
                if (bandalart.isCompleted && !bandalart.title.isNullOrEmpty()) {
                    val isBandalartCompleted = checkCompletedBandalartId(bandalart.id)
                    if (isBandalartCompleted) {
                        delay(500L)
                        requestCapture()
                        delay(500L)
                        navigateToComplete(
                            bandalart.id,
                            bandalart.title,
                            bandalart.profileEmoji.orEmpty(),
                            _uiState.value.bandalartChartUrl.orEmpty(),
                        )
                    }
                }
            }
        }
    }

    fun onAction(action: HomeUiAction) {
        when (action) {
            is HomeUiAction.OnListClick -> showBandalartListBottomSheet()
            is HomeUiAction.OnSaveClick -> requestCapture()
            is HomeUiAction.OnDeleteClick -> showBandalartDeleteDialog()
            is HomeUiAction.OnEmojiSelected -> updateBandalartEmoji(
                action.bandalartId,
                action.cellId,
                action.updateBandalartEmojiModel,
            )

            is HomeUiAction.OnShareButtonClick -> requestShare()
            is HomeUiAction.OnAddClick -> createBandalart()
            is HomeUiAction.OnMenuClick -> showDropDownMenu()
            is HomeUiAction.OnDropDownMenuDismiss -> hideDropDownMenu()
            is HomeUiAction.OnEmojiClick -> showEmojiBottomSheet()
            is HomeUiAction.OnBandalartListItemClick -> {
                setRecentBandalartId(action.key)
                getBandalart(action.key)
                hideBottomSheet()
            }

            is HomeUiAction.OnBandalartCellClick -> handleBandalartCellClick(
                action.cellType,
                action.isMainCellTitleEmpty,
                action.cellData,
            )

            is HomeUiAction.OnCloseButtonClick -> hideBottomSheet()
            is HomeUiAction.OnAppTitleClick -> showAppVersion()
            is HomeUiAction.OnDismiss -> hideBottomSheet()
            is HomeUiAction.OnCellTitleUpdate -> updateCellTitle(action.title, action.locale)
            is HomeUiAction.OnEmojiSelect -> updateEmoji(action.emoji)
            is HomeUiAction.OnColorSelect -> updateThemeColor(action.mainColor, action.subColor)
            is HomeUiAction.OnDueDateSelect -> updateDueDate(action.date)
            is HomeUiAction.OnDescriptionUpdate -> updateDescription(action.description)
            is HomeUiAction.OnCompletionUpdate -> updateCompletion(action.isCompleted)
            is HomeUiAction.OnDeleteBandalart -> deleteBandalart(action.bandalartId)
            is HomeUiAction.OnDeleteCell -> deleteCell(action.cellId)
            is HomeUiAction.OnCancelClick -> hideDialog()
            is HomeUiAction.OnEmojiPickerClick -> expandEmojiPicker()
            is HomeUiAction.OnDatePickerClick -> expandDatePicker()
            is HomeUiAction.OnDeleteButtonClick -> showCellDeleteDialog()
            is HomeUiAction.OnCompleteButtonClick -> {
                updateCell(
                    bandalartId = action.bandalartId,
                    cellId = action.cellId,
                    cellType = action.cellType,
                )
            }
        }
    }

    private fun navigateToComplete(bandalartId: Long, title: String, profileEmoji: String, bandalartChart: String) {
        viewModelScope.launch {
            _uiEvent.send(
                HomeUiEvent.NavigateToComplete(
                    id = bandalartId,
                    title = title,
                    profileEmoji = profileEmoji,
                    bandalartChart = bandalartChart,
                ),
            )
        }
    }

    private fun getBandalartList() {
        viewModelScope.launch {
            bandalartRepository.getBandalartList()
                .map { list -> list.map { it.toUiModel() } }
                .collect { bandalartList ->
                    _uiState.update { it.copy(bandalartList = bandalartList.toImmutableList()) }

                    // 이전 반다라트 목록 상태 조회
                    val prevBandalartList = bandalartRepository.getPrevBandalartList()

                    // 새로 업데이트 된 상태와 이전 상태를 비교
                    val completedKeys = bandalartList.filter { bandalart ->
                        val prevBandalart = prevBandalartList.find { it.first == bandalart.id }
                        prevBandalart != null && !prevBandalart.second && bandalart.isCompleted
                    }.map { it.id }

                    // 이번에 목표를 달성한 반다라트가 존재하는 경우
                    if (completedKeys.isNotEmpty()) {
                        getBandalart(completedKeys[0], isBandalartCompleted = true)
                        return@collect
                    }

                    // 데이터를 동기화
                    bandalartList.forEach { bandalart ->
                        bandalartRepository.upsertBandalartId(bandalart.id, bandalart.isCompleted)
                    }

                    // 반다라트 목록이 존재하지 않을 경우, 새로운 반다라트를 생성
                    if (bandalartList.isEmpty()) {
                        createBandalart()
                        return@collect
                    } else {
                        // 반다라트 목록이 존재할 경우
                        // 가장 최근에 확인한 반다라트 표를 화면에 띄우는 경우
                        val recentBandalartId = getRecentBandalartId()
                        // 가장 최근에 확인한 반다라트 표가 존재 하는 경우
                        if (bandalartList.any { it.id == recentBandalartId }) {
                            getBandalart(recentBandalartId)
                        }
                        // 가장 최근에 확인한 반다라트 표가 존재 하지 않을 경우
                        else {
                            // 목록에 가장 첫번째 표를 화면에 띄움
                            getBandalart(bandalartList[0].id)
                        }
                    }
                }
        }
    }

    private fun getBandalart(bandalartId: Long, isBandalartCompleted: Boolean = false) {
        viewModelScope.launch {
            updateSkeletonState(true)
            bandalartRepository.getBandalart(bandalartId).let { bandalart ->
                _uiState.update {
                    it.copy(
                        bandalartData = bandalart.toUiModel(),
                        bottomSheet = null,
                        isBandalartCompleted = isBandalartCompleted,
                    )
                }
                getBandalartMainCell(bandalartId)
            }
            updateSkeletonState(false)
        }
    }

    private fun getBandalartMainCell(bandalartId: Long) {
        viewModelScope.launch {
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
            _uiState.update {
                it.copy(
                    bandalartCellData = BandalartCellEntity(
                        id = mainCell?.id ?: 0L,
                        title = mainCell?.title,
                        description = mainCell?.description,
                        dueDate = mainCell?.dueDate,
                        isCompleted = mainCell?.isCompleted ?: false,
                        parentId = mainCell?.parentId,
                        children = children,
                    ),
                )
            }
            updateSkeletonState(false)
        }
    }

    private fun createBandalart() {
        viewModelScope.launch {
            if (_uiState.value.bandalartList.size + 1 > 5) {
                _uiEvent.send(HomeUiEvent.ShowToast(getString(Res.string.limit_create_bandalart)))
                return@launch
            }

            updateSkeletonState(true)
            bandalartRepository.createBandalart()?.let { bandalart ->
                hideBottomSheet()
                // 새로운 반다라트를 생성하면 화면에 생성된 반다라트 표를 보여주도록 id 를 전달
                getBandalart(bandalart.id)
                // 새로운 반다라트의 키를 최근에 확인한 반다라트로 저장
                setRecentBandalartId(bandalart.id)
                // 새로운 반다라트를 로컬에 저장
                upsertBandalartId(bandalart.id)
                _uiEvent.send(HomeUiEvent.ShowSnackbar(getString(Res.string.create_bandalart)))
            }
        }
    }

    fun saveBandalartImage(bitmap: ImageBitmap) {
        viewModelScope.launch {
            _uiEvent.send(HomeUiEvent.SaveBandalart(bitmap))
            hideDropDownMenu()
        }
    }

    private fun deleteBandalart(bandalartId: Long) {
        viewModelScope.launch {
            updateSkeletonState(true)
            bandalartRepository.deleteBandalart(bandalartId)
            hideModal()
            hideDropDownMenu()
            deleteBandalartId(bandalartId)
            _uiEvent.send(HomeUiEvent.ShowSnackbar(getString(Res.string.delete_bandalart)))
        }
    }

    private fun updateBandalartEmoji(
        bandalartId: Long,
        cellId: Long,
        updateBandalartEmojiModel: UpdateBandalartEmojiEntity,
    ) {
        viewModelScope.launch {
            bandalartRepository.updateBandalartEmoji(bandalartId, cellId, updateBandalartEmojiModel)
            hideBottomSheet()
        }
    }

    private fun requestShare() {
        _uiState.update { it.copy(isSharing = true) }
    }

    private fun requestCapture() {
        _uiState.update { it.copy(isCapturing = true) }
    }

    private fun clearShareState() {
        _uiState.update { it.copy(isSharing = false) }
    }

    private fun clearCaptureState() {
        _uiState.update { it.copy(isCapturing = false) }
    }

    fun shareBandalart(bitmap: ImageBitmap) {
        viewModelScope.launch {
            clearShareState()
            _uiEvent.send(HomeUiEvent.ShareBandalart(bitmap))
        }
    }

    fun captureBandalart(bitmap: ImageBitmap) {
        viewModelScope.launch {
            clearCaptureState()
            _uiEvent.send(HomeUiEvent.CaptureBandalart(bitmap))
        }
    }

    private fun updateSkeletonState(flag: Boolean) {
        _uiState.update { it.copy(isShowSkeleton = flag) }
    }

    private suspend fun getRecentBandalartId(): Long {
        return bandalartRepository.getRecentBandalartId()
    }

    private fun setRecentBandalartId(bandalartId: Long) {
        viewModelScope.launch {
            bandalartRepository.setRecentBandalartId(bandalartId)
        }
    }

    private fun upsertBandalartId(bandalartId: Long, isCompleted: Boolean = false) {
        viewModelScope.launch {
            bandalartRepository.upsertBandalartId(bandalartId, isCompleted)
        }
    }

    private suspend fun checkCompletedBandalartId(bandalartId: Long): Boolean {
        return withContext(viewModelScope.coroutineContext) {
            bandalartRepository.checkCompletedBandalartId(bandalartId)
        }
    }

    private fun deleteBandalartId(bandalartId: Long) {
        viewModelScope.launch {
            bandalartRepository.deleteCompletedBandalartId(bandalartId)
        }
    }

    fun updateBandalartChartUrl(url: String) {
        _uiState.update { it.copy(bandalartChartUrl = url) }
    }

    // TODO 태스크셀이고 상위 서브셀이 비어있을 때(테스트셀이 자신의 부모를 알고있어야 구현 가능 함)
    private fun handleBandalartCellClick(
        cellType: CellType,
        isMainCellTitleEmpty: Boolean,
        cellData: BandalartCellEntity,
    ) {
        when {
            cellType != CellType.MAIN && isMainCellTitleEmpty -> {
                viewModelScope.launch {
                    _uiEvent.send(HomeUiEvent.ShowToast(getString(Res.string.please_input_main_goal)))
                }
            }

            else -> {
                _uiState.value.bandalartData?.let { bandalartData ->
                    _uiState.update {
                        it.copy(
                            clickedCellData = cellData,
                            clickedCellType = cellType,
                            bottomSheet = BottomSheetState.Cell(
                                initialCellData = cellData,
                                cellData = cellData,
                                initialBandalartData = bandalartData,
                                bandalartData = bandalartData,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun updateCellTitle(title: String, locale: Locale) {
        val maxLength = when (locale.language) {
            Language.KOREAN, Language.JAPANESE -> 15
            else -> 24
        }

        _uiState.update {
            val currentBottomSheet = (it.bottomSheet as? BottomSheetState.Cell) ?: return@update it
            val validatedTitle = if (title.length > maxLength) {
                currentBottomSheet.cellData.title ?: ""
            } else title

            it.copy(
                bottomSheet = currentBottomSheet.copy(
                    cellData = currentBottomSheet.cellData.copy(title = validatedTitle),
                ),
            )
        }
    }

    private fun updateEmoji(emoji: String) {
        _uiState.update {
            val currentBottomSheet = (it.bottomSheet as? BottomSheetState.Cell) ?: return@update it
            it.copy(
                bottomSheet = currentBottomSheet.copy(
                    bandalartData = currentBottomSheet.bandalartData.copy(profileEmoji = emoji),
                ),
            )
        }
        shrinkEmojiPicker()
    }

    private fun updateThemeColor(mainColor: String, subColor: String) {
        _uiState.update {
            val currentBottomSheet = it.bottomSheet as? BottomSheetState.Cell ?: return@update it
            it.copy(
                bottomSheet = currentBottomSheet.copy(
                    bandalartData = currentBottomSheet.bandalartData.copy(
                        mainColor = mainColor,
                        subColor = subColor,
                    ),
                ),
            )
        }
    }

    private fun updateDueDate(date: String) {
        _uiState.update {
            val currentBottomSheet = (it.bottomSheet as? BottomSheetState.Cell) ?: return@update it
            it.copy(
                bottomSheet = currentBottomSheet.copy(
                    cellData = currentBottomSheet.cellData.copy(dueDate = date),
                ),
            )
        }
        shrinkDatePicker()
    }

    private fun updateDescription(description: String) {
        _uiState.update {
            val currentBottomSheet = (it.bottomSheet as? BottomSheetState.Cell) ?: return@update it
            val validatedDescription = if (description.length > 1000) {
                currentBottomSheet.cellData.description
            } else {
                description
            }
            it.copy(
                bottomSheet = currentBottomSheet.copy(
                    cellData = currentBottomSheet.cellData.copy(description = validatedDescription),
                ),
            )
        }
    }

    private fun updateCompletion(isCompleted: Boolean) {
        _uiState.update {
            val currentBottomSheet = (it.bottomSheet as? BottomSheetState.Cell) ?: return@update it
            it.copy(
                bottomSheet = currentBottomSheet.copy(
                    cellData = currentBottomSheet.cellData.copy(isCompleted = isCompleted),
                ),
            )
        }
    }

    private fun deleteCell(cellId: Long) {
        viewModelScope.launch {
            bandalartRepository.deleteBandalartCell(cellId)
            hideModal()
        }
    }

    private fun updateCell(
        bandalartId: Long,
        cellId: Long,
        cellType: CellType,
    ) {
        val bottomSheetData = _uiState.value.bottomSheet as? BottomSheetState.Cell ?: return
        val cellData = bottomSheetData.cellData
        val bandalartData = bottomSheetData.bandalartData

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
                        profileEmoji = bandalartData.profileEmoji,
                        mainColor = bandalartData.mainColor,
                        subColor = bandalartData.subColor,
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

    private fun updateMainCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartMainCellModel: UpdateBandalartMainCellEntity,
    ) {
        viewModelScope.launch {
            bandalartRepository.updateBandalartMainCell(bandalartId, cellId, updateBandalartMainCellModel)
            bandalartRepository.getBandalart(bandalartId)
            hideBottomSheet()
        }
    }

    private fun updateSubCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartSubCellModel: UpdateBandalartSubCellEntity,
    ) {
        viewModelScope.launch {
            bandalartRepository.updateBandalartSubCell(bandalartId, cellId, updateBandalartSubCellModel)
            hideBottomSheet()
        }
    }

    private fun updateTaskCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartTaskCellModel: UpdateBandalartTaskCellEntity,
    ) {
        viewModelScope.launch {
            bandalartRepository.updateBandalartTaskCell(bandalartId, cellId, updateBandalartTaskCellModel)
            hideBottomSheet()
        }
    }

    private fun showDropDownMenu() {
        _uiState.update { it.copy(isDropDownMenuOpened = true) }
    }

    private fun showEmojiBottomSheet() {
        _uiState.value.bandalartData?.let { bandalartData ->
            _uiState.update {
                it.copy(
                    bottomSheet = BottomSheetState.Emoji(
                        bandalartId = bandalartData.id,
                        cellId = bandalartData.id,
                        currentEmoji = bandalartData.profileEmoji,
                    ),
                )
            }
        }
    }

    private fun showBandalartListBottomSheet() {
        _uiState.update {
            it.copy(
                bottomSheet = BottomSheetState.BandalartList(
                    bandalartList = it.bandalartList,
                    currentBandalartId = it.bandalartData?.id ?: return,
                ),
            )
        }
    }

    private fun showBandalartDeleteDialog() {
        _uiState.update { it.copy(dialog = DialogState.BandalartDelete) }
    }

    private fun showCellDeleteDialog() {
        _uiState.update {
            it.copy(
                dialog = DialogState.CellDelete(
                    cellType = it.clickedCellType,
                    cellTitle = (it.bottomSheet as? BottomSheetState.Cell)?.cellData?.title,
                ),
            )
        }
    }

    private fun expandEmojiPicker() {
        _uiState.update {
            val currentBottomSheet = it.bottomSheet as? BottomSheetState.Cell ?: return@update it
            it.copy(
                bottomSheet = currentBottomSheet.copy(isEmojiPickerOpened = true),
            )
        }
    }

    private fun expandDatePicker() {
        _uiState.update {
            val currentSheet = it.bottomSheet as? BottomSheetState.Cell ?: return@update it
            it.copy(
                bottomSheet = currentSheet.copy(isDatePickerOpened = true),
            )
        }
    }

    private fun shrinkEmojiPicker() {
        _uiState.update {
            val currentBottomSheet = it.bottomSheet as? BottomSheetState.Cell ?: return@update it
            it.copy(
                bottomSheet = currentBottomSheet.copy(isEmojiPickerOpened = false),
            )
        }
    }

    private fun shrinkDatePicker() {
        _uiState.update {
            val currentSheet = it.bottomSheet as? BottomSheetState.Cell ?: return@update it
            it.copy(
                bottomSheet = currentSheet.copy(isDatePickerOpened = false),
            )
        }
    }

    private fun hideDropDownMenu() {
        _uiState.update { it.copy(isDropDownMenuOpened = false) }
    }

    private fun hideModal() {
        hideDialog()
        hideBottomSheet()
    }

    private fun hideDialog() {
        _uiState.update { it.copy(dialog = null) }
    }

    private fun hideBottomSheet() {
        _uiState.update { it.copy(bottomSheet = null) }
    }

    private fun showAppVersion() {
        viewModelScope.launch {
            _uiEvent.send(HomeUiEvent.ShowAppVersion)
        }
    }

    suspend fun setLastRejectedUpdateVersion(versionCode: Int) {
        inAppUpdateRepository.setLastRejectedUpdateVersion(versionCode)
    }

    suspend fun isUpdateAlreadyRejected(versionCode: Int): Boolean {
        return inAppUpdateRepository.isUpdateAlreadyRejected(versionCode)
    }
}
