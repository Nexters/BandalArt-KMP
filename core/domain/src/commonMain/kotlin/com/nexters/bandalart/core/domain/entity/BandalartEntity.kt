package com.nexters.bandalart.core.domain.entity

import androidx.compose.runtime.Stable

/**
 * 반다라트 생성
 *
 * @param id 셀 고유 id
 * @param mainColor 메인 테마 색상 (Sub Cell, Task Cell 이면 null)
 * @param subColor 서브 테마 색상 (Sub Cell, Task Cell 이면 null)
 * @param profileEmoji 프로필 이모지 (Sub Cell, Task Cell 이면 null)
 * @param completionRatio 목표 달성률(%)
 */

@Stable
data class BandalartEntity(
    val id: Long,
    val mainColor: String,
    val subColor: String,
    val profileEmoji: String?,
    val title: String?,
    val description: String?,
    val dueDate: String?,
    val isCompleted: Boolean,
    val completionRatio: Int,
)
