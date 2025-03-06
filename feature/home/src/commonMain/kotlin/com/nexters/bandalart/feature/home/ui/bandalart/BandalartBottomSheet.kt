package com.nexters.bandalart.feature.home.ui.bandalart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.arrow_forward_description
import bandalart.core.designsystem.generated.resources.bottomsheet_color
import bandalart.core.designsystem.generated.resources.bottomsheet_completed
import bandalart.core.designsystem.generated.resources.bottomsheet_description
import bandalart.core.designsystem.generated.resources.bottomsheet_description_placeholder
import bandalart.core.designsystem.generated.resources.bottomsheet_duedate
import bandalart.core.designsystem.generated.resources.bottomsheet_duedate_placeholder
import bandalart.core.designsystem.generated.resources.bottomsheet_in_completed
import bandalart.core.designsystem.generated.resources.bottomsheet_is_completed
import bandalart.core.designsystem.generated.resources.bottomsheet_title
import bandalart.core.designsystem.generated.resources.bottomsheet_title_placeholder
import bandalart.core.designsystem.generated.resources.edit_description
import bandalart.core.designsystem.generated.resources.empty_emoji_description
import bandalart.core.designsystem.generated.resources.ic_edit
import bandalart.core.designsystem.generated.resources.ic_empty_emoji
import com.nexters.bandalart.core.common.extension.LocalDateTime
import com.nexters.bandalart.core.common.extension.noRippleClickable
import com.nexters.bandalart.core.common.extension.toLocalDateTime
import com.nexters.bandalart.core.common.extension.toStringLocalDateTime
import com.nexters.bandalart.core.common.getLocale
import com.nexters.bandalart.core.designsystem.theme.Gray100
import com.nexters.bandalart.core.designsystem.theme.Gray300
import com.nexters.bandalart.core.designsystem.theme.Gray400
import com.nexters.bandalart.core.designsystem.theme.Gray700
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.ui.NavigationBarHeightDp
import com.nexters.bandalart.core.ui.ThemeColor
import com.nexters.bandalart.core.ui.getNavigationBarPadding
import com.nexters.bandalart.feature.home.model.CellType
import com.nexters.bandalart.feature.home.viewmodel.BottomSheetState
import com.nexters.bandalart.feature.home.viewmodel.HomeUiAction
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandalartBottomSheet(
    bandalartId: Long,
    cellType: CellType,
    isBlankCell: Boolean,
    cellData: BandalartCellEntity,
    bottomSheetData: BottomSheetState.Cell,
    onHomeUiAction: (HomeUiAction) -> Unit,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val isCompleteButtonEnabled by remember(
        bottomSheetData.cellData.title,
        bottomSheetData.initialCellData,
        bottomSheetData.cellData,
        bottomSheetData.initialBandalartData,
        bottomSheetData.bandalartData,
    ) {
        derivedStateOf {
            val isTitleNotEmpty = bottomSheetData.cellData.title?.trim()?.isNotEmpty() == true
            val isCellDataChanged = bottomSheetData.initialCellData != bottomSheetData.cellData
            val isBandalartDataChanged = bottomSheetData.initialBandalartData != bottomSheetData.bandalartData

            isTitleNotEmpty && (isCellDataChanged || isBandalartDataChanged)
        }
    }

    val showTopGradient by remember(scrollState.value) {
        derivedStateOf { scrollState.value > 0 }
    }
    val showBottomGradient by remember(scrollState.value) {
        derivedStateOf { scrollState.value < scrollState.maxValue }
    }

    ModalBottomSheet(
        onDismissRequest = {
            onHomeUiAction(HomeUiAction.OnDismiss)
        },
        modifier = Modifier
            .wrapContentSize()
            .statusBarsPadding(),
        sheetState = bottomSheetState,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .background(White)
                .navigationBarsPadding()
                .noRippleClickable { focusManager.clearFocus() },
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            BottomSheetTopBar(
                cellType = cellType,
                isBlankCell = isBlankCell,
                onCloseClick = {
                    onHomeUiAction(HomeUiAction.OnDismiss)
                },
            )
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .verticalScroll(scrollState)
                        .padding(start = 20.dp, top = 40.dp, end = 20.dp),
                ) {
                    BottomSheetSubTitleText(text = stringResource(Res.string.bottomsheet_title))
                    Spacer(modifier = Modifier.height(11.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (cellType == CellType.MAIN) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(end = 16.dp),
                            ) {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(52.dp)
                                            .aspectRatio(1f)
                                            .background(Gray100)
                                            .clickable {
                                                onHomeUiAction(HomeUiAction.OnEmojiPickerClick)
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (bottomSheetData.bandalartData.profileEmoji.isNullOrEmpty()) {
                                            Icon(
                                                imageVector = vectorResource(Res.drawable.ic_empty_emoji),
                                                contentDescription = stringResource(Res.string.empty_emoji_description),
                                                tint = Color.Unspecified,
                                            )
                                        } else {
                                            Text(
                                                text = bottomSheetData.bandalartData.profileEmoji,
                                                fontSize = 22.sp,
                                            )
                                        }
                                    }
                                }
                                Icon(
                                    imageVector = vectorResource(Res.drawable.ic_edit),
                                    contentDescription = stringResource(Res.string.edit_description),
                                    tint = Color.Unspecified,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 4.dp, y = 4.dp),
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            BandalartTextField(
                                value = bottomSheetData.cellData.title ?: "",
                                onValueChange = { title ->
                                    onHomeUiAction(
                                        HomeUiAction.OnCellTitleUpdate(title, getLocale()),
                                    )
                                },
                                placeholder = stringResource(Res.string.bottomsheet_title_placeholder),
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                thickness = 1.dp,
                                color = Gray300,
                            )
                        }
                    }
                    AnimatedVisibility(visible = bottomSheetData.isEmojiPickerOpened) {
                        Column {
                            BandalartEmojiPicker(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .padding(top = 4.dp)
                                    .animateContentSize(
                                        animationSpec = tween(
                                            durationMillis = 300,
                                            easing = LinearOutSlowInEasing,
                                        ),
                                    ),
                                currentEmoji = bottomSheetData.bandalartData.profileEmoji,
                                isBottomSheet = false,
                                onEmojiSelect = { selectedEmoji ->
                                    onHomeUiAction(HomeUiAction.OnEmojiSelect(selectedEmoji))
                                },
                            )
                        }
                    }
                    if (cellType == CellType.MAIN) {
                        Spacer(modifier = Modifier.height(22.dp))
                        BottomSheetSubTitleText(text = stringResource(Res.string.bottomsheet_color))
                        BandalartColorPicker(
                            initColor = ThemeColor(
                                mainColor = bottomSheetData.bandalartData.mainColor,
                                subColor = bottomSheetData.bandalartData.subColor,
                            ),
                            onColorSelect = { themeColor ->
                                onHomeUiAction(
                                    HomeUiAction.OnColorSelect(
                                        themeColor.mainColor,
                                        themeColor.subColor,
                                    ),
                                )
                            },
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                    Spacer(modifier = Modifier.height(25.dp))
                    BottomSheetSubTitleText(text = stringResource(Res.string.bottomsheet_duedate))
                    Spacer(modifier = Modifier.height(12.dp))
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .clickable {
                                    onHomeUiAction(HomeUiAction.OnDatePickerClick)
                                },
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (bottomSheetData.cellData.dueDate.isNullOrEmpty()) {
                                BottomSheetContentPlaceholder(text = stringResource(Res.string.bottomsheet_duedate_placeholder))
                            } else {
                                BottomSheetContentText(text = bottomSheetData.cellData.dueDate!!.toStringLocalDateTime(getLocale()))
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(Res.string.arrow_forward_description),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(24.dp),
                                tint = Gray400,
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = Gray300,
                        )
                    }
                    AnimatedVisibility(visible = bottomSheetData.isDatePickerOpened) {
                        BandalartDatePicker(
                            onDueDateSelect = { dueDateResult ->
                                onHomeUiAction(HomeUiAction.OnDueDateSelect(dueDateResult.toString()))
                            },
                            currentDueDate = bottomSheetData.cellData.dueDate?.toLocalDateTime()
                                ?: LocalDateTime.now(),
                        )
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                    BottomSheetSubTitleText(text = stringResource(Res.string.bottomsheet_description))
                    Spacer(modifier = Modifier.height(12.dp))
                    Column {
                        BandalartTextField(
                            value = bottomSheetData.cellData.description ?: "",
                            onValueChange = { description ->
                                onHomeUiAction(HomeUiAction.OnDescriptionUpdate(description))
                            },
                            placeholder = stringResource(Res.string.bottomsheet_description_placeholder),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = Gray300,
                        )
                    }
                    if (cellType == CellType.TASK) {
                        Spacer(modifier = Modifier.height(28.dp))
                        BottomSheetSubTitleText(text = stringResource(Res.string.bottomsheet_is_completed))
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            BottomSheetContentText(
                                modifier = Modifier.align(Alignment.CenterStart),
                                text = if (bottomSheetData.cellData.isCompleted) stringResource(Res.string.bottomsheet_completed)
                                else stringResource(Res.string.bottomsheet_in_completed),
                            )
                            Switch(
                                checked = bottomSheetData.cellData.isCompleted,
                                onCheckedChange = { isCompleted ->
                                    onHomeUiAction(HomeUiAction.OnCompletionUpdate(isCompleted))
                                },
                                colors = SwitchDefaults.colors(
                                    uncheckedThumbColor = White,
                                    uncheckedTrackColor = Gray300,
                                    uncheckedBorderColor = Gray300,
                                    checkedThumbColor = White,
                                    checkedTrackColor = Gray700,
                                    checkedBorderColor = Gray700,
                                ),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .width(52.dp)
                                    .height(28.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                            .padding(horizontal = 8.dp)
                            .imePadding(),
                    ) {
                        if (!isBlankCell) {
                            BottomSheetDeleteButton(
                                onClick = {
                                    onHomeUiAction(HomeUiAction.OnDeleteButtonClick)
                                },
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(9.dp))
                        }
                        BottomSheetCompleteButton(
                            isEnabled = isCompleteButtonEnabled,
                            onClick = {
                                onHomeUiAction(
                                    HomeUiAction.OnCompleteButtonClick(
                                        bandalartId,
                                        cellData.id,
                                        cellType,
                                    ),
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(NavigationBarHeightDp + getNavigationBarPadding()))
                }

                if (showTopGradient) {
                    ScrollGradientOverlay(isTop = true)
                }

                if (showBottomGradient) {
                    ScrollGradientOverlay(
                        isTop = false,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

// @ComponentPreview
// @Composable
// private fun BandalartMainCellBottomSheetPreview() {
//     BandalartTheme {
//         BandalartBottomSheet(
//             bandalartId = 0L,
//             cellType = CellType.MAIN,
//             isBlankCell = false,
//             cellData = dummyBandalartCellData,
//             bottomSheetData = BottomSheetState.Cell(
//                 initialCellData = dummyBandalartCellData,
//                 cellData = dummyBandalartCellData,
//                 initialBandalartData = dummyBandalartData,
//                 bandalartData = dummyBandalartData,
//             ),
//             onHomeUiAction = {},
//         )
//     }
// }
//
// @ComponentPreview
// @Composable
// private fun BandalartSubCellBottomSheetPreview() {
//     BandalartTheme {
//         BandalartBottomSheet(
//             bandalartId = 0L,
//             cellType = CellType.SUB,
//             isBlankCell = false,
//             cellData = dummyBandalartCellData,
//             bottomSheetData = BottomSheetState.Cell(
//                 initialCellData = dummyBandalartCellData,
//                 cellData = dummyBandalartCellData,
//                 initialBandalartData = dummyBandalartData,
//                 bandalartData = dummyBandalartData,
//             ),
//             onHomeUiAction = {},
//         )
//     }
// }
//
// @ComponentPreview
// @Composable
// private fun BandalartTaskCellBottomSheetPreview() {
//     BandalartTheme {
//         BandalartBottomSheet(
//             bandalartId = 0L,
//             cellType = CellType.TASK,
//             isBlankCell = true,
//             cellData = dummyBandalartCellData,
//             bottomSheetData = BottomSheetState.Cell(
//                 initialCellData = dummyBandalartCellData,
//                 cellData = dummyBandalartCellData,
//                 initialBandalartData = dummyBandalartData,
//                 bandalartData = dummyBandalartData,
//             ),
//             onHomeUiAction = {},
//         )
//     }
// }
