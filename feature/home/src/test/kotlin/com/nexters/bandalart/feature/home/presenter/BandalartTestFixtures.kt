package com.nexters.bandalart.feature.home.presenter

import com.nexters.bandalart.core.domain.entity.BandalartEntity

internal fun bandalart(id: Long) = BandalartEntity(
    id = id,
    mainColor = "#3FFFBA",
    subColor = "#111827",
    profileEmoji = "🚀",
    title = "반다라트 $id",
    description = null,
    dueDate = null,
    isCompleted = false,
    completionRatio = 0,
)
