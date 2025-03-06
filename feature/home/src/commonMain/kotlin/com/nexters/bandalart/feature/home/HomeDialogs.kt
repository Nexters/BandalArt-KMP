package com.nexters.bandalart.feature.home

import androidx.compose.runtime.Composable
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.delete_bandalart_dialog_empty_title
import bandalart.core.designsystem.generated.resources.delete_bandalart_dialog_message
import bandalart.core.designsystem.generated.resources.delete_bandalart_dialog_title
import bandalart.core.designsystem.generated.resources.delete_bandalart_maincell_dialog_message
import bandalart.core.designsystem.generated.resources.delete_bandalart_maincell_dialog_title
import bandalart.core.designsystem.generated.resources.delete_bandalart_subcell_dialog_message
import bandalart.core.designsystem.generated.resources.delete_bandalart_subcell_dialog_title
import bandalart.core.designsystem.generated.resources.delete_bandalart_taskcell_dialog_message
import bandalart.core.designsystem.generated.resources.delete_bandalart_taskcell_dialog_title
import com.nexters.bandalart.feature.home.model.CellType
import com.nexters.bandalart.feature.home.ui.bandalart.BandalartDeleteAlertDialog
import com.nexters.bandalart.feature.home.viewmodel.DialogState
import com.nexters.bandalart.feature.home.viewmodel.HomeUiAction
import com.nexters.bandalart.feature.home.viewmodel.HomeUiState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeDialogs(
    uiState: HomeUiState,
    onHomeUiAction: (HomeUiAction) -> Unit,
) {
    when (uiState.dialog) {
        is DialogState.BandalartDelete -> {
            uiState.bandalartData?.let { bandalart ->
                BandalartDeleteAlertDialog(
                    title = if (bandalart.title.isNullOrEmpty()) {
                        stringResource(Res.string.delete_bandalart_dialog_empty_title)
                    } else {
                        stringResource(Res.string.delete_bandalart_dialog_title, bandalart.title)
                    },
                    message = stringResource(Res.string.delete_bandalart_dialog_message),
                    onDeleteClick = {
                        onHomeUiAction(HomeUiAction.OnDeleteBandalart(bandalart.id))
                    },
                    onCancelClick = {
                        onHomeUiAction(HomeUiAction.OnCancelClick)
                    },
                )
            }
        }
        is DialogState.CellDelete -> {
            uiState.clickedCellData?.let { cellData ->
                BandalartDeleteAlertDialog(
                    title = when (uiState.clickedCellType) {
                        CellType.MAIN -> stringResource(Res.string.delete_bandalart_maincell_dialog_title, cellData.title ?: "")
                        CellType.SUB -> stringResource(Res.string.delete_bandalart_subcell_dialog_title, cellData.title ?: "")
                        else -> stringResource(Res.string.delete_bandalart_taskcell_dialog_title, cellData.title ?: "")
                    },
                    message = when (uiState.clickedCellType) {
                        CellType.MAIN -> stringResource(Res.string.delete_bandalart_maincell_dialog_message)
                        CellType.SUB -> stringResource(Res.string.delete_bandalart_subcell_dialog_message)
                        else -> stringResource(Res.string.delete_bandalart_taskcell_dialog_message)
                    },
                    onDeleteClick = {
                        onHomeUiAction(HomeUiAction.OnDeleteCell(cellData.id))
                    },
                    onCancelClick = {
                        onHomeUiAction(HomeUiAction.OnCancelClick)
                    },
                )
            }
        }
        null -> {}
    }
}
