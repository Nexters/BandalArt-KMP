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
import zone.ien.hig.adaptive.AdaptiveTheme
import zone.ien.hig.adaptive.CupertinoThemeSpec
import zone.ien.hig.adaptive.ExperimentalAdaptiveApi
import zone.ien.hig.adaptive.MaterialThemeSpec
import zone.ien.hig.theme.darkColorScheme as cupertinoDarkColorScheme
import zone.ien.hig.theme.lightColorScheme as cupertinoLightColorScheme

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
        secondary = DarkSurfaceVariant,
        onSecondary = DarkOnSurface,
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
        secondary = Gray900,
        onSecondary = Color.White,
        outline = Gray300,
        outlineVariant = Gray100,
        surfaceContainer = Gray200,
    )

@OptIn(ExperimentalAdaptiveApi::class)
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
        ) {
            // hig-adaptive 컴포넌트가 iOS는 Cupertino, 그 외는 Material로 그리도록 테마 대상을 제공한다.
            // Material 스펙은 위 MaterialTheme 값을 그대로 넘겨 Android 화면이 바뀌지 않게 하고,
            // Cupertino 다크 여부는 설정 테마 모드를 반영한 darkTheme을 따른다.
            AdaptiveTheme(
                material = MaterialThemeSpec.Default(),
                cupertino =
                    CupertinoThemeSpec(
                        colorScheme = if (darkTheme) cupertinoDarkColorScheme() else cupertinoLightColorScheme(),
                    ),
                content = content,
            )
        }
    }
}
