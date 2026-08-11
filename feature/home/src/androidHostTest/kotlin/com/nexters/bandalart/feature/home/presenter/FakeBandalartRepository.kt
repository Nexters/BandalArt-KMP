/*
 * Copyright 2026 easyhooon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nexters.bandalart.feature.home.presenter

import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartEmojiEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartMainCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartSubCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartTaskCellEntity
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import com.nexters.bandalart.core.domain.template.BandalartTemplateId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeBandalartRepository(
    initialBandalarts: List<BandalartEntity>,
    recentBandalartId: Long = 0L,
    private val previousBandalartList: List<Pair<Long, Boolean>> =
        initialBandalarts.map { it.id to it.isCompleted },
    private val createdBandalart: BandalartEntity? = null,
    private val publishCreatedBandalartImmediately: Boolean = true,
    details: Map<Long, BandalartEntity> = initialBandalarts.associateBy { it.id },
    mainCells: Map<Long, BandalartCellEntity> = emptyMap(),
    childCells: Map<Long, List<BandalartCellEntity>> = emptyMap(),
    private val beforeTaskCellUpdate: suspend () -> Unit = {},
    private val afterTaskCellUpdate: suspend () -> Unit = {},
    private val beforeBandalartLoad: suspend (Long) -> Unit = {},
    private val beforeCompletionUpdate: suspend () -> Unit = {},
    private val taskCellUpdateError: Throwable? = null,
) : BandalartRepository {
    private val bandalartFlow = MutableStateFlow(initialBandalarts)
    private val details = details.toMutableMap()
    private val mainCells = mainCells.toMutableMap()
    private val childCells = childCells.toMutableMap()
    private var unpublishedCreatedBandalart: BandalartEntity? = null

    private val recentBandalartFlow = MutableStateFlow(recentBandalartId)

    var recentBandalartId: Long
        get() = recentBandalartFlow.value
        private set(value) {
            recentBandalartFlow.value = value
        }

    var createCalls: Int = 0
        private set
    val createdTemplateIds = mutableListOf<BandalartTemplateId?>()

    val completionUpdates = mutableListOf<Pair<Long, Boolean>>()
    val deletedBandalartIds = mutableListOf<Long>()
    val deletedCellIds = mutableListOf<Long>()
    val deletedCompletionIds = mutableListOf<Long>()
    val emojiUpdates = mutableListOf<EmojiUpdate>()
    var mainCellUpdate: MainCellUpdate? = null
        private set
    var subCellUpdate: SubCellUpdate? = null
        private set
    var taskCellUpdate: TaskCellUpdate? = null
        private set
    var taskCellUpdateCalls: Int = 0
        private set

    override suspend fun createBandalart(templateId: BandalartTemplateId?): BandalartEntity? {
        createCalls += 1
        createdTemplateIds += templateId
        return createdBandalart?.also { bandalart ->
            details[bandalart.id] = bandalart
            if (publishCreatedBandalartImmediately) {
                bandalartFlow.value = bandalartFlow.value + bandalart
            } else {
                unpublishedCreatedBandalart = bandalart
            }
        }
    }

    fun publishCreatedBandalart() {
        val bandalart = unpublishedCreatedBandalart ?: return
        unpublishedCreatedBandalart = null
        bandalartFlow.value = bandalartFlow.value + bandalart
    }

    fun publishBandalartRevision(
        bandalart: BandalartEntity,
        mainCell: BandalartCellEntity? = null,
    ) {
        details[bandalart.id] = bandalart
        if (mainCell != null) mainCells[bandalart.id] = mainCell
        bandalartFlow.value =
            bandalartFlow.value.map { current ->
                if (current.id == bandalart.id) bandalart else current
            }
    }

    fun publishTaskCellRevision(taskCell: BandalartCellEntity) {
        childCells.replaceTaskCell(taskCell)
    }

    override fun getBandalartList(): Flow<List<BandalartEntity>> = bandalartFlow

    override suspend fun getBandalart(bandalartId: Long): BandalartEntity {
        beforeBandalartLoad(bandalartId)
        return requireNotNull(details[bandalartId])
    }

    override suspend fun getBandalartMainCell(bandalartId: Long): BandalartCellEntity? = mainCells[bandalartId]

    override suspend fun getChildCells(parentId: Long): List<BandalartCellEntity> = childCells[parentId].orEmpty()

    override suspend fun setRecentBandalartId(recentBandalartId: Long) {
        this.recentBandalartId = recentBandalartId
    }

    override suspend fun getRecentBandalartId(): Long = recentBandalartId

    override fun observeRecentBandalartId(): Flow<Long> = recentBandalartFlow

    override suspend fun getPrevBandalartList(): List<Pair<Long, Boolean>> = previousBandalartList

    override suspend fun upsertBandalartId(
        bandalartId: Long,
        isCompleted: Boolean,
    ) {
        beforeCompletionUpdate()
        completionUpdates += bandalartId to isCompleted
    }

    override suspend fun deleteBandalart(bandalartId: Long) {
        deletedBandalartIds += bandalartId
        details.remove(bandalartId)
        bandalartFlow.value = bandalartFlow.value.filterNot { it.id == bandalartId }
    }

    override suspend fun updateBandalartMainCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartMainCellEntity: UpdateBandalartMainCellEntity,
    ) {
        mainCellUpdate =
            MainCellUpdate(
                bandalartId = bandalartId,
                cellId = cellId,
                entity = updateBandalartMainCellEntity,
            )
    }

    override suspend fun updateBandalartSubCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartSubCellEntity: UpdateBandalartSubCellEntity,
    ) {
        subCellUpdate =
            SubCellUpdate(
                bandalartId = bandalartId,
                cellId = cellId,
                entity = updateBandalartSubCellEntity,
            )
    }

    override suspend fun updateBandalartTaskCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartTaskCellEntity: UpdateBandalartTaskCellEntity,
    ) {
        taskCellUpdateCalls += 1
        beforeTaskCellUpdate()
        taskCellUpdateError?.let { throw it }
        taskCellUpdate =
            TaskCellUpdate(
                bandalartId = bandalartId,
                cellId = cellId,
                entity = updateBandalartTaskCellEntity,
            )
        childCells.replaceTaskCell(
            BandalartCellEntity(
                id = cellId,
                title = updateBandalartTaskCellEntity.title,
                description = updateBandalartTaskCellEntity.description,
                dueDate = updateBandalartTaskCellEntity.dueDate,
                isCompleted = updateBandalartTaskCellEntity.isCompleted ?: false,
                parentId =
                    childCells.values
                        .flatten()
                        .firstOrNull { it.id == cellId }
                        ?.parentId,
            ),
        )
        afterTaskCellUpdate()
    }

    override suspend fun setTaskCompleted(
        bandalartId: Long,
        subGoalId: Long,
        taskId: Long,
        completed: Boolean,
    ): Boolean = error("Not used")

    override suspend fun updateBandalartEmoji(
        bandalartId: Long,
        cellId: Long,
        updateBandalartEmojiEntity: UpdateBandalartEmojiEntity,
    ) {
        emojiUpdates +=
            EmojiUpdate(
                bandalartId = bandalartId,
                cellId = cellId,
                entity = updateBandalartEmojiEntity,
            )
    }

    override suspend fun deleteBandalartCell(cellId: Long) {
        deletedCellIds += cellId
    }

    override suspend fun checkCompletedBandalartId(bandalartId: Long): Boolean = error("Not used")

    override suspend fun deleteCompletedBandalartId(bandalartId: Long) {
        deletedCompletionIds += bandalartId
    }

    data class MainCellUpdate(
        val bandalartId: Long,
        val cellId: Long,
        val entity: UpdateBandalartMainCellEntity,
    )

    data class SubCellUpdate(
        val bandalartId: Long,
        val cellId: Long,
        val entity: UpdateBandalartSubCellEntity,
    )

    data class TaskCellUpdate(
        val bandalartId: Long,
        val cellId: Long,
        val entity: UpdateBandalartTaskCellEntity,
    )

    data class EmojiUpdate(
        val bandalartId: Long,
        val cellId: Long,
        val entity: UpdateBandalartEmojiEntity,
    )

    private fun MutableMap<Long, List<BandalartCellEntity>>.replaceTaskCell(taskCell: BandalartCellEntity) {
        entries.forEach { entry ->
            if (entry.value.any { it.id == taskCell.id }) {
                entry.setValue(entry.value.map { current -> if (current.id == taskCell.id) taskCell else current })
                return
            }
        }
    }
}
