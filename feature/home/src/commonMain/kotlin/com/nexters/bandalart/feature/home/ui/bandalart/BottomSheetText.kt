package com.nexters.bandalart.feature.home.ui.bandalart

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.bottomsheet_header_maincell_edit_title
import bandalart.core.designsystem.generated.resources.bottomsheet_header_maincell_enter_title
import bandalart.core.designsystem.generated.resources.bottomsheet_header_subcell_edit_title
import bandalart.core.designsystem.generated.resources.bottomsheet_header_subcell_enter_title
import bandalart.core.designsystem.generated.resources.bottomsheet_header_taskcell_edit_title
import bandalart.core.designsystem.generated.resources.bottomsheet_header_taskcell_enter_title
import com.nexters.bandalart.core.designsystem.theme.Gray400
import com.nexters.bandalart.core.designsystem.theme.Gray600
import com.nexters.bandalart.core.designsystem.theme.Gray900
import com.nexters.bandalart.feature.home.model.CellType
import org.jetbrains.compose.resources.stringResource

// TODO 텍스트들을 굳이 Composable 로 뺄 필요가 있나 생각(font 를 정의해서 넣으면 되는데)
@Composable
fun BottomSheetTitleText(
    cellType: CellType,
    isBlankCell: Boolean,
    modifier: Modifier = Modifier,
) {
    val titleResId = when {
        isBlankCell -> when (cellType) {
            CellType.MAIN -> Res.string.bottomsheet_header_maincell_enter_title
            CellType.SUB -> Res.string.bottomsheet_header_subcell_enter_title
            CellType.TASK -> Res.string.bottomsheet_header_taskcell_enter_title
        }
        else -> when (cellType) {
            CellType.MAIN -> Res.string.bottomsheet_header_maincell_edit_title
            CellType.SUB -> Res.string.bottomsheet_header_subcell_edit_title
            CellType.TASK -> Res.string.bottomsheet_header_taskcell_edit_title
        }
    }

    Text(
        text = stringResource(titleResId),
        color = Gray900,
        fontSize = 16.sp,
        fontWeight = FontWeight.W700,
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        letterSpacing = (-0.32).sp,
        lineHeight = 22.4.sp,
    )
}

@Composable
fun BottomSheetSubTitleText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = Gray600,
        fontSize = 12.sp,
        fontWeight = FontWeight.W700,
        modifier = modifier,
        textAlign = TextAlign.Start,
        letterSpacing = (-0.24).sp,
        lineHeight = 22.4.sp,
    )
}

@Composable
fun BottomSheetContentPlaceholder(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = Gray400,
        fontSize = 16.sp,
        fontWeight = FontWeight.W400,
        modifier = modifier,
        textAlign = TextAlign.Start,
        letterSpacing = (-0.32).sp,
        lineHeight = 22.4.sp,
    )
}

@Composable
fun BottomSheetContentText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        textAlign = TextAlign.Start,
        color = Gray600,
        fontSize = 16.sp,
        fontWeight = FontWeight.W600,
        modifier = modifier,
        letterSpacing = (-0.32).sp,
        lineHeight = 22.4.sp,
    )
}

@Composable
fun BottomSheetButtonText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        textAlign = TextAlign.Start,
        color = color,
        fontSize = 16.sp,
        fontWeight = FontWeight.W700,
        modifier = modifier,
        letterSpacing = (-0.32).sp,
        lineHeight = 21.sp,
    )
}

// @ComponentPreview
// @Composable
// private fun BottomSheetMainCellTitleTextPreview() {
//     BandalartTheme {
//         BottomSheetTitleText(
//             cellType = CellType.MAIN,
//             isBlankCell = false,
//         )
//     }
// }
//
// @ComponentPreview
// @Composable
// private fun BottomSheetSubCellTitleTextPreview() {
//     BandalartTheme {
//         BottomSheetTitleText(
//             cellType = CellType.SUB,
//             isBlankCell = false,
//         )
//     }
// }
//
// @ComponentPreview
// @Composable
// private fun BottomSheetBlankCellTitleTextPreview() {
//     BandalartTheme {
//         BottomSheetTitleText(
//             cellType = CellType.TASK,
//             isBlankCell = true,
//         )
//     }
// }
//
// @ComponentPreview
// @Composable
// private fun BottomSheetSubTitleTextPreview() {
//     BandalartTheme {
//         BottomSheetSubTitleText(text = stringResource(Res.string.bottomsheet_title))
//     }
// }
//
// @ComponentPreview
// @Composable
// private fun BottomSheetContentPlaceholderPreview() {
//     BandalartTheme {
//         BottomSheetContentPlaceholder(text = stringResource(Res.string.bottomsheet_title_placeholder))
//     }
// }
//
// @ComponentPreview
// @Composable
// private fun BottomSheetContentTextPreview() {
//     BandalartTheme {
//         BottomSheetContentText(text = stringResource(Res.string.bottomsheet_completed))
//     }
// }
//
// @ComponentPreview
// @Composable
// private fun BottomSheetButtonTextPreview() {
//     BandalartTheme {
//         BottomSheetButtonText(
//             text = stringResource(Res.string.bottomsheet_done),
//             color = Gray400,
//         )
//     }
// }
