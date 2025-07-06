package com.nexters.bandalart.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.dp
import com.nexters.bandalart.core.common.extension.captureToGraphicsLayer
import com.nexters.bandalart.core.common.utils.UiText
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.designsystem.theme.Gray100
import com.nexters.bandalart.core.designsystem.theme.Gray50
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartEmojiEntity
import com.nexters.bandalart.core.ui.DevicePreview
import com.nexters.bandalart.feature.home.HomeScreen.Event
import com.nexters.bandalart.feature.home.model.BandalartUiModel
import com.nexters.bandalart.feature.home.model.CellType
import com.nexters.bandalart.feature.home.model.dummy.dummyBandalartChartData
import com.nexters.bandalart.feature.home.model.dummy.dummyBandalartData
import com.nexters.bandalart.feature.home.model.dummy.dummyBandalartList
import com.nexters.bandalart.feature.home.ui.HomeHeader
import com.nexters.bandalart.feature.home.ui.HomeShareButton
import com.nexters.bandalart.feature.home.ui.HomeTopBar
import com.nexters.bandalart.feature.home.ui.bandalart.BandalartChart
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import dagger.hilt.android.components.ActivityRetainedComponent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.parcelize.Parcelize
import java.util.Locale

@Parcelize
data object HomeScreen : Screen {
    data class State(
        val bandalartList: ImmutableList<BandalartUiModel> = persistentListOf(),
        val bandalartData: BandalartUiModel? = null,
        val bandalartCellData: BandalartCellEntity? = null,
        val bandalartChartUrl: String? = null,
        val isBandalartCompleted: Boolean = false,
        val bottomSheet: BottomSheetState? = null,
        val dialog: DialogState? = null,
        val isSharing: Boolean = false,
        val isCapturing: Boolean = false,
        val isDropDownMenuOpened: Boolean = false,
        val clickedCellType: CellType = CellType.MAIN,
        val clickedCellData: BandalartCellEntity? = null,
        val updateVersionCode: Int? = null,
        val showUpdateConfirm: Boolean = false,
        val sideEffect: SideEffect? = null,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed interface BottomSheetState {
        data class Cell(
            val initialCellData: BandalartCellEntity,
            val cellData: BandalartCellEntity,
            val initialBandalartData: BandalartUiModel,
            val bandalartData: BandalartUiModel,
            val isDatePickerOpened: Boolean = false,
            val isEmojiPickerOpened: Boolean = false,
        ) : BottomSheetState

        data class BandalartList(
            val bandalartList: ImmutableList<BandalartUiModel>,
            val currentBandalartId: Long,
        ) : BottomSheetState

        data class Emoji(
            val bandalartId: Long,
            val cellId: Long,
            val currentEmoji: String?,
        ) : BottomSheetState
    }

    sealed interface DialogState {
        data object BandalartDelete : DialogState
        data class CellDelete(
            val cellType: CellType,
            val cellTitle: String?,
        ) : DialogState
    }

    sealed interface SideEffect {
        data class ShowSnackbar(val message: UiText) : SideEffect
        data class ShowToast(val message: UiText) : SideEffect
        data object ShowAppVersion : SideEffect
        data class SaveImage(val bitmap: ImageBitmap) : SideEffect
        data class ShareImage(val bitmap: ImageBitmap) : SideEffect
        data class CaptureImage(val bitmap: ImageBitmap) : SideEffect
    }

    sealed interface Event : CircuitUiEvent {
        data class OnShareRequested(val bitmap: ImageBitmap) : Event
        data class OnSaveRequested(val bitmap: ImageBitmap) : Event
        data class OnCaptureRequested(val bitmap: ImageBitmap) : Event
        data class OnUpdateCheck(val versionCode: Int) : Event
        data object OnUpdateDownloadComplete : Event
        data class OnUpdateDownloaded(val doUpdate: Boolean) : Event
        data object OnUpdateCanceled : Event
        data class CaptureFinished(val bandalartChartUrl: String) : Event
        data object InitSideEffect : Event

        // HomeScreen UiAction
        data object OnListClick : Event
        data object OnSaveClick : Event
        data object OnDeleteClick : Event
        data class OnEmojiSelected(
            val bandalartId: Long,
            val cellId: Long,
            val updateBandalartEmojiModel: UpdateBandalartEmojiEntity,
        ) : Event

        data object OnShareButtonClick : Event
        data object OnAddClick : Event
        data object OnMenuClick : Event
        data object OnDropDownMenuDismiss : Event
        data object OnEmojiClick : Event
        data class OnBandalartListItemClick(val key: Long) : Event
        data class OnBandalartCellClick(
            val cellType: CellType,
            val isMainCellTitleEmpty: Boolean,
            val cellData: BandalartCellEntity,
        ) : Event

        data object OnCloseButtonClick : Event
        data object OnAppTitleClick : Event

        // BottomSheet UiAction
        data object OnDismiss : Event
        data class OnCellTitleUpdate(val title: String, val locale: Locale) : Event
        data class OnEmojiSelect(val emoji: String) : Event
        data class OnColorSelect(val mainColor: String, val subColor: String) : Event
        data object OnDatePickerClick : Event
        data class OnDueDateSelect(val date: String) : Event
        data class OnDescriptionUpdate(val description: String) : Event
        data class OnCompletionUpdate(val isCompleted: Boolean) : Event
        data class OnDeleteBandalart(val bandalartId: Long) : Event
        data class OnDeleteCell(val cellId: Long) : Event
        data object OnCancelClick : Event
        data object OnEmojiPickerClick : Event
        data object OnDeleteButtonClick : Event
        data class OnCompleteButtonClick(
            val bandalartId: Long,
            val cellId: Long,
            val cellType: CellType,
        ) : Event
    }
}

// TODO 서브 셀을 먼저 채워야 태스크 셀을 채울 수 있도록 validation 추가
// TODO 텍스트를 컴포저블로 각각 분리하지 말고, 폰트를 적용하는 방식으로 변경
@CircuitInject(HomeScreen::class, ActivityRetainedComponent::class)
@Composable
internal fun Home(
    state: HomeScreen.State,
    modifier: Modifier = Modifier,
) {
    val eventSink = state.eventSink
    val homeGraphicsLayer = rememberGraphicsLayer()
    val completeGraphicsLayer = rememberGraphicsLayer()
    val snackbarHostState = remember { SnackbarHostState() }

    HandleAppUpdate(
        state = state,
        snackbarHostState = snackbarHostState,
        eventSink = eventSink,
    )

    HandleHomeEffects(
        state = state,
        snackbarHostState = snackbarHostState,
        homeGraphicsLayer = homeGraphicsLayer,
        completeGraphicsLayer = completeGraphicsLayer,
        eventSink = eventSink,
    )

    HomeBottomSheets(
        state = state,
        eventSink = eventSink,
    )

    HomeDialogs(
        state = state,
        eventSink = eventSink,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Gray50),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            HomeTopBar(
                bandalartCount = state.bandalartList.size,
                eventSink = eventSink,
            )
            HorizontalDivider(
                thickness = 1.dp,
                color = Gray100,
            )
            Column(
                modifier = Modifier
                    .captureToGraphicsLayer(homeGraphicsLayer)
                    .background(Gray50),
            ) {
                if (state.bandalartCellData != null && state.bandalartData != null) {
                    HomeHeader(
                        bandalartData = state.bandalartData,
                        cellData = state.bandalartCellData,
                        isDropDownMenuOpened = state.isDropDownMenuOpened,
                        eventSink = eventSink,
                    )
                    BandalartChart(
                        bandalartData = state.bandalartData,
                        bandalartCellData = state.bandalartCellData,
                        eventSink = eventSink,
                        modifier = Modifier
                            .captureToGraphicsLayer(completeGraphicsLayer)
                            .background(Gray50),
                    )
                }
                Spacer(modifier = Modifier.height(64.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            HomeShareButton(
                onShareButtonClick = {
                    eventSink(Event.OnShareButtonClick)
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@DevicePreview
@Composable
private fun HomeSingleBandalartPreview() {
    BandalartTheme {
        Home(
            state = HomeScreen.State(
                bandalartList = listOf(dummyBandalartList[0]).toImmutableList(),
                bandalartData = dummyBandalartData,
                bandalartCellData = dummyBandalartChartData,
                eventSink = {},
            ),
        )
    }
}

@DevicePreview
@Composable
private fun HomeMultipleBandalartPreview() {
    BandalartTheme {
        Home(
            state = HomeScreen.State(
                bandalartList = dummyBandalartList.toImmutableList(),
                bandalartData = dummyBandalartData,
                bandalartCellData = dummyBandalartChartData,
                eventSink = {},
            ),
        )
    }
}
