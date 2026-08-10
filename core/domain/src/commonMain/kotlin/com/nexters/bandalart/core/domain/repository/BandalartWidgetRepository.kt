package com.nexters.bandalart.core.domain.repository

import com.nexters.bandalart.core.domain.entity.BandalartWidgetSnapshot

interface BandalartWidgetRepository {
    suspend fun getSnapshot(
        bandalartId: Long,
        subGoalId: Long?,
    ): BandalartWidgetSnapshot?

    suspend fun setTaskCompleted(
        bandalartId: Long,
        subGoalId: Long,
        taskId: Long,
        completed: Boolean,
    ): BandalartWidgetSnapshot?
}
