package com.nexters.bandalart.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.krona_one_regular
import bandalart.core.designsystem.generated.resources.neurimbo_gothic_regular
import bandalart.core.designsystem.generated.resources.pretendard_bold
import bandalart.core.designsystem.generated.resources.pretendard_medium
import bandalart.core.designsystem.generated.resources.pretendard_regular
import bandalart.core.designsystem.generated.resources.pretendard_semi_bold
import org.jetbrains.compose.resources.Font

@Composable
fun pretendardFontFamily() =
    FontFamily(
        Font(Res.font.pretendard_bold, FontWeight.Bold, FontStyle.Normal),
        Font(Res.font.pretendard_medium, FontWeight.Medium, FontStyle.Normal),
        Font(Res.font.pretendard_regular, FontWeight.Normal, FontStyle.Normal),
        Font(Res.font.pretendard_semi_bold, FontWeight.SemiBold, FontStyle.Normal),
    )

@Composable
fun neurimboGothicRegularFontFamily() =
    FontFamily(
        Font(Res.font.neurimbo_gothic_regular, FontWeight.Normal, FontStyle.Normal),
    )

@Composable
fun koronaOneRegularFontFamily() =
    FontFamily(
        Font(Res.font.krona_one_regular, FontWeight.Normal, FontStyle.Normal),
    )

@Composable
fun BottomSheetContent() =
    TextStyle(
        color = Gray900,
        fontFamily = pretendardFontFamily(),
        fontWeight = FontWeight.W600,
        fontSize = 16.sp,
        letterSpacing = -(0.32).sp,
        lineHeight = 22.4.sp,
    )
