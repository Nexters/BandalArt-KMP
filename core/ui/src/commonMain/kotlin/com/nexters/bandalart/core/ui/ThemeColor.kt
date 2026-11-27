package com.nexters.bandalart.core.ui

import androidx.compose.runtime.Stable

@Stable
data class ThemeColor(
    val mainColor: String,
    val subColor: String,
)

val allColor =
    listOf(
        // 네온 (기존 색, 순서 유지)
        ThemeColor("#3FFFBA", "#111827"),
        ThemeColor("#3FF3FF", "#111827"),
        ThemeColor("#93FF3F", "#111827"),
        ThemeColor("#FBFF3F", "#111827"),
        ThemeColor("#FFB423", "#111827"),
        ThemeColor("#FF9DF5", "#111827"),
        // 파스텔
        ThemeColor("#FFB3B3", "#111827"),
        ThemeColor("#FFD6A5", "#111827"),
        ThemeColor("#FFF1A8", "#111827"),
        ThemeColor("#C9F2B8", "#111827"),
        ThemeColor("#B8E3FF", "#111827"),
        ThemeColor("#D6C8FF", "#111827"),
        // 차분한 톤
        ThemeColor("#F0B5A8", "#111827"),
        ThemeColor("#E3C08D", "#111827"),
        ThemeColor("#B8C99A", "#111827"),
        ThemeColor("#9FC7C1", "#111827"),
        ThemeColor("#B0C5E2", "#111827"),
        ThemeColor("#CFBADB", "#111827"),
    )
