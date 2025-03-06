package com.nexters.bandalart.core.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// https://sungbin.land/jetpack-compose-windowinsets-fa8f286f092b
val NavigationBarHeightDp
    @Composable
    get() = with(LocalDensity.current) {
        WindowInsets.systemBars.getBottom(this).toDp()
    }

val StatusBarAndActionBarHeightDp
    @Composable
    get() = with(LocalDensity.current) {
        WindowInsets.systemBars.getTop(this).toDp()
    }

// https://stackoverflow.com/questions/75123079/how-do-i-detect-which-type-of-navigation-bar-is-active
@Composable
fun getNavigationBarPadding(): Dp {
    return if (NavigationBarHeightDp == 0.dp) 32.dp else NavigationBarHeightDp - 16.dp
}
