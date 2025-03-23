package com.nexters.bandalart.feature.home.ui.bandalart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.add_description
import bandalart.core.designsystem.generated.resources.complete_description
import bandalart.core.designsystem.generated.resources.home_main_cell
import bandalart.core.designsystem.generated.resources.home_sub_cell
import bandalart.core.designsystem.generated.resources.ic_cell_check
import com.nexters.bandalart.core.common.extension.toColor
import com.nexters.bandalart.core.designsystem.theme.Gray400
import com.nexters.bandalart.core.designsystem.theme.Gray500
import com.nexters.bandalart.core.designsystem.theme.Gray600
import com.nexters.bandalart.core.designsystem.theme.Gray900
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.feature.home.model.BandalartUiModel
import com.nexters.bandalart.feature.home.model.CellType
import com.nexters.bandalart.feature.home.viewmodel.HomeUiAction
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

data class CellInfo(
    val isSubCell: Boolean = false,
    val colIndex: Int = 2,
    val rowIndex: Int = 2,
    val colCnt: Int = 1,
    val rowCnt: Int = 1,
    // TODO 상위 셀 정보 추가 필요(서브 목표가 비어 있을 경우, 태스크 셀을 먼저 작성할 수 없도록 validation)
    val parentCell: BandalartCellEntity? = null,
)

data class SubCell(
    val rowCnt: Int,
    val colCnt: Int,
    val subCellRowIndex: Int,
    val subCellColIndex: Int,
    val subCellData: BandalartCellEntity?,
)

@Composable
fun BandalartCell(
    bandalartData: BandalartUiModel,
    cellType: CellType,
    cellData: BandalartCellEntity,
    onHomeUiAction: (HomeUiAction) -> Unit,
    modifier: Modifier = Modifier,
    cellInfo: CellInfo = CellInfo(),
    outerPadding: Dp = 3.dp,
    innerPadding: Dp = 2.dp,
    mainCellPadding: Dp = 1.dp,
) {
    Box(
        modifier = modifier
            .padding(
                start = if (cellType == CellType.MAIN) mainCellPadding else if (cellInfo.colIndex == 0) outerPadding else innerPadding,
                end = if (cellType == CellType.MAIN) mainCellPadding else if (cellInfo.colIndex == cellInfo.colCnt - 1) outerPadding else innerPadding,
                top = if (cellType == CellType.MAIN) mainCellPadding else if (cellInfo.rowIndex == 0) outerPadding else innerPadding,
                bottom = if (cellType == CellType.MAIN) mainCellPadding else if (cellInfo.rowIndex == cellInfo.rowCnt - 1) outerPadding else innerPadding,
            )
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(getCellBackgroundColor(bandalartData, cellType, cellData))
            .clickable {
                when (cellType) {
                    CellType.MAIN -> onHomeUiAction(HomeUiAction.OnBandalartCellClick(CellType.MAIN, bandalartData.titleText.isEmpty(), cellData))
                    CellType.SUB -> onHomeUiAction(HomeUiAction.OnBandalartCellClick(CellType.SUB, bandalartData.titleText.isEmpty(), cellData))
                    else -> onHomeUiAction(HomeUiAction.OnBandalartCellClick(CellType.TASK, bandalartData.titleText.isEmpty(), cellData))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        CellContent(
            cellType = cellType,
            cellData = cellData,
            bandalartData = bandalartData,
        )
    }
}

@Composable
private fun CellContent(
    cellType: CellType,
    cellData: BandalartCellEntity,
    bandalartData: BandalartUiModel,
) {
    when (cellType) {
        CellType.MAIN -> MainCellContent(cellData, bandalartData)
        CellType.SUB -> SubCellContent(cellData, bandalartData)
        else -> TaskCellContent(cellData)
    }
}

@Composable
private fun MainCellContent(
    cellData: BandalartCellEntity,
    bandalartData: BandalartUiModel,
) {
    val cellTextColor = bandalartData.subColor.toColor()

    if (cellData.title.isNullOrEmpty()) {
        EmptyMainCellContent(cellTextColor)
    } else {
        FilledCellContent(
            title = cellData.title!!,
            isCompleted = cellData.isCompleted,
            textColor = cellTextColor,
            fontWeight = FontWeight.W700,
        )
    }
}

@Composable
private fun SubCellContent(
    cellData: BandalartCellEntity,
    bandalartData: BandalartUiModel,
) {
    val cellTextColor = bandalartData.mainColor.toColor()

    if (cellData.title.isNullOrEmpty()) {
        EmptySubCellContent(cellTextColor)
    } else {
        FilledCellContent(
            title = cellData.title!!,
            isCompleted = cellData.isCompleted,
            textColor = cellTextColor,
            fontWeight = FontWeight.W700,
        )
    }
}

@Composable
private fun TaskCellContent(cellData: BandalartCellEntity) {
    val cellTextColor = if (cellData.isCompleted) Gray600 else Gray900

    if (cellData.title.isNullOrEmpty()) {
        EmptyTaskContent()
    } else {
        FilledCellContent(
            title = cellData.title!!,
            isCompleted = cellData.isCompleted,
            textColor = cellTextColor,
            fontWeight = FontWeight.W500,
        )
    }
}

@Composable
private fun EmptyMainCellContent(textColor: Color) {
    Box(contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CellText(
                cellText = stringResource(Res.string.home_main_cell),
                cellTextColor = textColor,
                fontWeight = FontWeight.W700,
            )
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(Res.string.add_description),
                tint = textColor,
                modifier = Modifier
                    .size(20.dp)
                    .offset(y = (-4).dp),
            )
        }
    }
}

@Composable
private fun EmptySubCellContent(textColor: Color) {
    Box(contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CellText(
                cellText = stringResource(Res.string.home_sub_cell),
                cellTextColor = textColor,
                fontWeight = FontWeight.W700,
            )
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(Res.string.add_description),
                tint = textColor,
                modifier = Modifier
                    .size(20.dp)
                    .offset(y = (-4).dp),
            )
        }
    }
}

@Composable
private fun EmptyTaskContent() {
    Icon(
        imageVector = Icons.Default.Add,
        contentDescription = stringResource(Res.string.add_description),
        tint = Gray500,
        modifier = Modifier.size(20.dp),
    )
}

@Composable
private fun FilledCellContent(
    title: String,
    isCompleted: Boolean,
    textColor: Color,
    fontWeight: FontWeight,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CellText(
            cellText = title,
            cellTextColor = textColor,
            fontWeight = fontWeight,
            textAlpha = if (isCompleted) 0.6f else 1f,
        )

        if (isCompleted) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_cell_check),
                contentDescription = stringResource(Res.string.complete_description),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp),
                tint = Color.Unspecified,
            )
        }
    }
}

private fun getCellBackgroundColor(
    bandalartData: BandalartUiModel,
    cellType: CellType,
    cellData: BandalartCellEntity,
): Color = when {
    // 메인 목표 달성
    cellType == CellType.MAIN && cellData.isCompleted -> bandalartData.mainColor.toColor().copy(alpha = 0.6f)
    // 메인 목표 미달성
    cellType == CellType.MAIN -> bandalartData.mainColor.toColor()
    // 서브 목표 달성
    cellType == CellType.SUB && cellData.isCompleted -> bandalartData.subColor.toColor().copy(alpha = 0.6f)
    // 서브 목표 미달성
    cellType == CellType.SUB -> bandalartData.subColor.toColor()
    // 태스크 목표 달성
    cellData.isCompleted -> Gray400
    else -> White
}

// @ComponentPreview
// @Composable
// private fun BandalartCellPreview() {
//     BandalartTheme {
//        BandalartCell(
//            bandalartData = BandalartUiModel(
//                id = 0L,
//                mainColor = "#FF3FFFBA",
//                subColor = "#FF111827",
//            ),
//            cellType = CellType.MAIN,
//            cellData = BandalartCellEntity(
//                title = "메인 목표",
//                isCompleted = false,
//            ),
//            onHomeUiAction = {},
//        )
//    }
// }
