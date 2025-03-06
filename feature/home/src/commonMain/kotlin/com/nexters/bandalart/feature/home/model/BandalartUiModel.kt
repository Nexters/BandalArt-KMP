package com.nexters.bandalart.feature.home.model

data class BandalartUiModel(
    val id: Long = 0L,
    val mainColor: String = "#3FFFBA",
    val subColor: String = "#111827",
    val profileEmoji: String? = "",
    val title: String? = "",
    val cellId: Long = 0L,
    val dueDate: String? = "",
    val isCompleted: Boolean = false,
    val completionRatio: Int = 0,
    val isGeneratedTitle: Boolean = false,
)
