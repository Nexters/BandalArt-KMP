package com.nexters.bandalart.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val DarkColorScheme =
    darkColorScheme(
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        primary = DarkOnSurface,
        onPrimary = Gray900,
        outline = DarkOutline,
        outlineVariant = DarkOutlineVariant,
        surfaceContainer = DarkOutlineVariant,
    )

private val LightColorScheme =
    lightColorScheme(
        background = Gray50,
        onBackground = Gray900,
        surface = Color.White,
        onSurface = Gray900,
        surfaceVariant = Gray100,
        onSurfaceVariant = Gray600,
        primary = Gray900,
        onPrimary = Color.White,
        outline = Gray300,
        outlineVariant = Gray100,
        surfaceContainer = Gray200,
    )

@Composable
fun BandalartTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    CompositionLocalProvider(
        LocalDensity provides Density(density = LocalDensity.current.density, fontScale = 1f),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = bandalartTypography(),
            content = content,
        )
    }
}
