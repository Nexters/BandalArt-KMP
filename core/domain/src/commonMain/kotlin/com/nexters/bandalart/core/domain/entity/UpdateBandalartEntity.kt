package com.nexters.bandalart.core.domain.entity

import androidx.compose.runtime.Stable

/**
 * 반다라트 메인 셀 수정
 *
 * @param title 셀 제목
 * @param description 셀 설명
 * @param dueDate 셀 마감일, 미설정인 경우 null
 * @param profileEmoji 셀 프로필 이모지
 * @param mainColor 셀 메인 색상
 * @param subColor 셀 서브 색상
 */
@Stable
data class UpdateBandalartMainCellEntity(
    val title: String?,
    val description: String?,
    val dueDate: String?,
    val profileEmoji: String?,
    val mainColor: String,
    val subColor: String,
)

/**
 * 반다라트 서브 셀 수정
 *
 * @param title 셀 제목
 * @param description 셀 설명
 * @param dueDate 셀 마감일, 미설정인 경우 null
 */
@Stable
data class UpdateBandalartSubCellEntity(
    val title: String?,
    val description: String?,
    val dueDate: String?,
)

/**
 * 반다라트 태스크 셀 수정
 *
 * @param title 셀 제목
 * @param description 셀 설명
 * @param dueDate 셀 마감일, 미설정인 경우 null
 * @param isCompleted 셀 완료 여부
 */
@Stable
data class UpdateBandalartTaskCellEntity(
    val title: String?,
    val description: String?,
    val dueDate: String?,
    val isCompleted: Boolean? = null,
)

/**
 * 반다라트 이모지 수정
 *
 * @param profileEmoji 반다라트 프로필 이모지
 */
@Stable
data class UpdateBandalartEmojiEntity(
    val profileEmoji: String?,
)
