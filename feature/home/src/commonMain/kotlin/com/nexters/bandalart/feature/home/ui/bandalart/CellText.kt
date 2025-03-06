package com.nexters.bandalart.feature.home.ui.bandalart

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily

// val cellLineBreak = LineBreak(
//    strategy = LineBreak.Strategy.Simple,
//    strictness = LineBreak.Strictness.Normal,
//    wordBreak = LineBreak.WordBreak.Phrase,
// )

@Composable
fun CellText(
    cellText: String,
    cellTextColor: Color,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    textAlpha: Float = 1f,
) {
    Text(
        text = cellText,
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .alpha(textAlpha),
        color = cellTextColor,
        fontFamily = pretendardFontFamily(),
        fontWeight = fontWeight,
        fontSize = 12.sp,
        lineHeight = 16.8.sp,
        letterSpacing = (-0.24).sp,
        textAlign = TextAlign.Center,
        // style = TextStyle(lineBreak = cellLineBreak),
    )
}

// @ComponentPreview
// @Composable
// private fun CellTextPreview() {
//     BandalartTheme {
//         CellText(
//             cellText = "완벽한 2024년",
//             cellTextColor = Gray900,
//             fontWeight = FontWeight.W700,
//         )
//     }
// }
