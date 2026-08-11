package com.nexters.bandalart.core.data.repository

import com.nexters.bandalart.core.database.BandalartDao
import com.nexters.bandalart.core.domain.entity.BandalartWidgetSnapshot
import com.nexters.bandalart.core.domain.entity.BandalartWidgetTask
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import com.nexters.bandalart.core.domain.repository.BandalartWidgetRepository

class DefaultBandalartWidgetRepository(
    private val bandalartRepository: BandalartRepository,
    private val bandalartDao: BandalartDao,
) : BandalartWidgetRepository {
    override suspend fun getSnapshot(
        bandalartId: Long,
        subGoalId: Long?,
    ): BandalartWidgetSnapshot? {
        val storedSnapshot = bandalartDao.findWidgetSnapshot(bandalartId, subGoalId) ?: return null
        return BandalartWidgetSnapshot(
            bandalartId = bandalartId,
            subGoalId = subGoalId,
            title = storedSnapshot.bandalart.title.orEmpty(),
            profileEmoji = storedSnapshot.bandalart.profileEmoji,
            completionRatio = storedSnapshot.bandalart.completionRatio,
            subGoalTitle = storedSnapshot.subGoal?.title,
            tasks =
                storedSnapshot.tasks.map { task ->
                    BandalartWidgetTask(
                        id = requireNotNull(task.id),
                        title = requireNotNull(task.title),
                        isCompleted = task.isCompleted,
                    )
                },
        )
    }

    override suspend fun setTaskCompleted(
        bandalartId: Long,
        subGoalId: Long,
        taskId: Long,
        completed: Boolean,
    ): BandalartWidgetSnapshot? {
        bandalartRepository.setTaskCompleted(
            bandalartId = bandalartId,
            subGoalId = subGoalId,
            taskId = taskId,
            completed = completed,
        )
        return getSnapshot(bandalartId, subGoalId)
    }
}
