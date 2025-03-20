package com.nexters.bandalart.feature.home.model

import androidx.compose.ui.text.input.TextFieldValue

data class BandalartUiModel(
    val id: Long = 0L,
    val mainColor: String = "#3FFFBA",
    val subColor: String = "#111827",
    val profileEmoji: String? = "",
    val title: TextFieldValue? = TextFieldValue(""),
    val cellId: Long = 0L,
    val dueDate: String? = "",
    val isCompleted: Boolean = false,
    val completionRatio: Int = 0,
    val isGeneratedTitle: Boolean = false,
) {
    val titleText: String
        get() = title?.text ?: ""

    val hasTitleText: Boolean
        get() = titleText.isNotEmpty()
}
