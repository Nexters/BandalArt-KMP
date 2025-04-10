package com.nexters.bandalart.core.ui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.bandalart
import com.nexters.bandalart.core.common.Language
import com.nexters.bandalart.core.common.getLocale
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.designsystem.theme.Gray900
import com.nexters.bandalart.core.designsystem.theme.koronaOneRegularFontFamily
import com.nexters.bandalart.core.designsystem.theme.neurimboGothicRegularFontFamily
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AppTitle(
    modifier: Modifier = Modifier,
) {
    when (getLocale().language) {
        Language.KOREAN -> {
            AppKoreanTitle(modifier = modifier)
        }

        Language.ENGLISH -> {
            AppEnglishTitle(modifier = modifier)
        }

        else -> {
            AppEnglishTitle(modifier = modifier)
        }
    }
}

@Composable
fun AppKoreanTitle(
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(Res.string.bandalart),
        color = Gray900,
        fontSize = 28.sp,
        fontWeight = FontWeight.W400,
        modifier = modifier,
        fontFamily = neurimboGothicRegularFontFamily(),
        lineHeight = 20.sp,
        letterSpacing = (-0.56).sp,
    )
}

@Composable
fun AppEnglishTitle(
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(Res.string.bandalart),
        color = Gray900,
        fontSize = 18.sp,
        fontWeight = FontWeight.W400,
        modifier = modifier,
        fontFamily = koronaOneRegularFontFamily(),
        lineHeight = 20.sp,
        letterSpacing = (-0.36).sp,
    )
}

// @ComponentPreview
@Preview
@Composable
private fun AppTitlePreview() {
    BandalartTheme {
        AppTitle()
    }
}

// @Preview(locale = "ko-rKR", showBackground = true)
@Preview
@Composable
private fun AppKoreanTitlePreview() {
    BandalartTheme {
        AppKoreanTitle()
    }
}

// @Preview(locale = "en-rUS", showBackground = true)
@Preview
@Composable
private fun AppEnglishTitlePreview() {
    BandalartTheme {
        AppEnglishTitle()
    }
}
