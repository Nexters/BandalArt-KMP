package com.nexters.bandalart.core.domain.repository

import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartEmojiEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartMainCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartSubCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartTaskCellEntity
import kotlinx.coroutines.flow.Flow

/** 반다라트 API */
interface BandalartRepository {
    /**
     * 반다라트 생성
     */
    suspend fun createBandalart(): BandalartEntity?

    /**
     * 반다라트 목록 조회
     */
    fun getBandalartList(): Flow<List<BandalartEntity>>

    /**
     * 반다라트 조회
     *
     * @param bandalartId 반다라트 고유 id
     */
    suspend fun getBandalart(bandalartId: Long): BandalartEntity

    /**
     * 반다라트 삭제
     *
     * @param bandalartId 반다라트 고유 id
     */
    suspend fun deleteBandalart(bandalartId: Long)

    /**
     * 반다라트 메인 셀 조회
     *
     * @param bandalartId 반다라트 고유 id
     */
    suspend fun getBandalartMainCell(bandalartId: Long): BandalartCellEntity?

    /**
     * 반다라트 하위 셀 조회
     *
     * @param parentId 부모 셀의 고유 id
     */
    suspend fun getChildCells(parentId: Long): List<BandalartCellEntity>

    /**
     * 반다라트 메인 셀 수정
     *
     * @param bandalartId 반다라트 고유 id
     * @param cellId 메인 셀 고유 id
     */
    suspend fun updateBandalartMainCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartMainCellEntity: UpdateBandalartMainCellEntity,
    )

    /**
     * 반다라트 서브 셀 수정
     *
     * @param bandalartId 반다라트 고유 id
     * @param cellId 서브 셀 고유 id
     */
    suspend fun updateBandalartSubCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartSubCellEntity: UpdateBandalartSubCellEntity,
    )

    /**
     * 반다라트 태스크 셀 수정
     *
     * @param bandalartId 빈디라트 고유 id
     * @param cellId 테스크 셀 고유 id
     */
    suspend fun updateBandalartTaskCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartTaskCellEntity: UpdateBandalartTaskCellEntity,
    )

    /**
     * 반다라트 이모지 수정
     *
     * @param bandalartId 빈디라트 고유 id
     * @param cellId 테스크 셀 고유 id
     */
    suspend fun updateBandalartEmoji(
        bandalartId: Long,
        cellId: Long,
        updateBandalartEmojiEntity: UpdateBandalartEmojiEntity,
    )

    /**
     * 반다라트 셀 삭제
     * @param cellId 셀 고유 id
     */
    suspend fun deleteBandalartCell(cellId: Long)

    /**
     * 최근에 수정한 반다라트 고유 id 수정
     * @param recentBandalartId 최근에 수정한 반다라트 고유 id
     */
    suspend fun setRecentBandalartId(recentBandalartId: Long)

    /**
     * 최근에 수정한 반다라트 고유 id 조회
     */
    suspend fun getRecentBandalartId(): Long

    /**
     * 바로 직전 상태의 반다라트 id 와 목표달성 여부를 가진 목록을 조회
     */
    suspend fun getPrevBandalartList(): List<Pair<Long, Boolean>>

    /**
     * 반다라트 id 와 반다라트의 목표 달성 여부를 갱신 및 추가
     * @param bandalartId 반다라트 고유 id
     * @param isCompleted 반다라트 완료 여부
     */
    suspend fun upsertBandalartId(bandalartId: Long, isCompleted: Boolean)

    /**
     * 이번에 목표를 달성한 반다라트 인지 여부 확인
     * @param bandalartId 반다라트 고유 id
     */
    suspend fun checkCompletedBandalartId(bandalartId: Long): Boolean

    /**
     * 삭제한 반다라트를 제거
     * @param bandalartId 반다라트 고유 id
     */
    suspend fun deleteCompletedBandalartId(bandalartId: Long)
}
