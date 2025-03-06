package com.nexters.bandalart.core.domain.entity

import androidx.compose.runtime.Stable

/**
 * 반다라트 셀 조회
 *
 * @param id 셀 고유 id
 * @param title 셀 제목
 * @param description 셀 설명
 * @param dueDate 셀 마감일, 미설정인 경우 null
 * @param isCompleted 셀 완료 여부
 * @param parentId 부모 셀 고유 키 (Main Cell 이면 null)
 */

@Stable
data class BandalartCellEntity(
    val id: Long = 0L,
    val bandalartId: Long? = 0L,
    val title: String? = null,
    val description: String? = null,
    val dueDate: String? = null,
    val isCompleted: Boolean = false,
    val parentId: Long? = 0L,
    val children: List<BandalartCellEntity> = emptyList(),
)
