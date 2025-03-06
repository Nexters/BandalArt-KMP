package com.nexters.bandalart.core.data.mapper

import com.nexters.bandalart.core.database.entity.BandalartCellDBEntity
import com.nexters.bandalart.core.database.entity.BandalartDBEntity
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.BandalartEntity

fun BandalartDBEntity.toEntity() = BandalartEntity(
    id = id ?: 0L,
    mainColor = mainColor,
    subColor = subColor,
    profileEmoji = profileEmoji,
    title = title,
    description = description,
    dueDate = dueDate,
    isCompleted = isCompleted,
    completionRatio = completionRatio,
)

fun BandalartCellDBEntity.toEntity() = BandalartCellEntity(
    id = id ?: 0L,
    bandalartId = bandalartId,
    title = title,
    description = description,
    dueDate = dueDate,
    isCompleted = isCompleted,
    parentId = parentId,
)
