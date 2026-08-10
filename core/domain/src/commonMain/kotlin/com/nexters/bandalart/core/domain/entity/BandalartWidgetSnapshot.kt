package com.nexters.bandalart.core.domain.entity

data class BandalartWidgetSnapshot(
    val bandalartId: Long,
    val subGoalId: Long?,
    val title: String,
    val profileEmoji: String?,
    val completionRatio: Int,
    val subGoalTitle: String?,
    val tasks: List<BandalartWidgetTask>,
)

data class BandalartWidgetTask(
    val id: Long,
    val title: String,
    val isCompleted: Boolean,
)
