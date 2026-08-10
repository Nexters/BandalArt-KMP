package com.nexters.bandalart.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable

private val DefaultTypography = Typography()

@Composable
internal fun bandalartTypography(): Typography {
    val fontFamily = pretendardFontFamily()

    return Typography(
        displayLarge = DefaultTypography.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = DefaultTypography.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = DefaultTypography.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = DefaultTypography.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = DefaultTypography.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = DefaultTypography.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = DefaultTypography.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = DefaultTypography.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = DefaultTypography.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = DefaultTypography.labelSmall.copy(fontFamily = fontFamily),
    )
}
