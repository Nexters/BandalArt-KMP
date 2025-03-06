package com.nexters.bandalart.feature.home.ui.bandalart

import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.unit.dp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.bottomsheet_delete
import bandalart.core.designsystem.generated.resources.bottomsheet_done
import com.nexters.bandalart.core.designsystem.theme.Gray200
import com.nexters.bandalart.core.designsystem.theme.Gray400
import com.nexters.bandalart.core.designsystem.theme.Gray900
import org.jetbrains.compose.resources.stringResource

@Composable
fun BottomSheetDeleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = IconButtonColors(Gray200, Gray900, Gray200, Gray900),
    ) {
        BottomSheetButtonText(
            text = stringResource(Res.string.bottomsheet_delete),
            color = Gray900,
        )
    }
}

@Composable
fun BottomSheetCompleteButton(
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = IconButtonColors(Gray900, White, Gray200, Gray400),
        enabled = isEnabled,
    ) {
        BottomSheetButtonText(
            text = stringResource(Res.string.bottomsheet_done),
            color = if (isEnabled) White else Gray400,
        )
    }
}

// @ComponentPreview
// @Composable
// private fun BottomSheetDeleteButtonPreview() {
//     BandalartTheme {
//         BottomSheetDeleteButton(
//             onClick = {},
//             modifier = Modifier
//                 .fillMaxWidth()
//                 .padding(horizontal = 16.dp),
//         )
//     }
// }
//
// @ComponentPreview
// @Composable
// private fun BottomSheetCompleteButtonPreview() {
//     BandalartTheme {
//         BottomSheetCompleteButton(
//             isEnabled = false,
//             onClick = {},
//             modifier = Modifier
//                 .fillMaxWidth()
//                 .padding(horizontal = 16.dp),
//         )
//     }
// }
