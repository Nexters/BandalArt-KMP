package com.nexters.bandalart.core.ui

import androidx.compose.runtime.Stable

@Stable
data class ThemeColor(
    val mainColor: String,
    val subColor: String,
)

val allColor = listOf(
    ThemeColor("#3FFFBA", "#111827"),
    ThemeColor("#3FF3FF", "#111827"),
    ThemeColor("#93FF3F", "#111827"),
    ThemeColor("#FBFF3F", "#111827"),
    ThemeColor("#FFB423", "#111827"),
    ThemeColor("#FF9DF5", "#111827"),
)
