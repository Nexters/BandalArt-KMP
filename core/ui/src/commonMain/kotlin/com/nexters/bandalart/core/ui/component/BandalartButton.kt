package com.nexters.bandalart.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexters.bandalart.core.common.extension.clickableSingle
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily

@Composable
fun BandalartButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clickableSingle(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = White,
            fontSize = 16.sp,
            fontWeight = FontWeight.W700,
            modifier = Modifier.padding(horizontal = 32.dp),
            fontFamily = pretendardFontFamily(),
            letterSpacing = (-0.32).sp,
        )
    }
}

// @ComponentPreview
// @Composable
// private fun BandalartButtonPreview() {
//     BandalartTheme {
//         BandalartButton(
//             onClick = {},
//             text = stringResource(Res.string.complete_save),
//             modifier = Modifier.fillMaxWidth(),
//         )
//     }
// }
