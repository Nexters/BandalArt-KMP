package com.nexters.bandalart.feature.home.mapper

import androidx.compose.ui.text.input.TextFieldValue
import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.feature.home.model.BandalartUiModel

fun BandalartEntity.toUiModel() = BandalartUiModel(
    id = id,
    mainColor = mainColor,
    subColor = subColor,
    profileEmoji = profileEmoji,
    title = title?.let { TextFieldValue(it) },
    dueDate = dueDate,
    isCompleted = isCompleted,
    completionRatio = completionRatio,
)
