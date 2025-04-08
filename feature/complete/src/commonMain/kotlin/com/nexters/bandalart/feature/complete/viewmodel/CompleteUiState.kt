package com.nexters.bandalart.feature.complete.viewmodel

/**
 * CompleteUiState
 *
 * @param id 반다라트 고유 id
 * @param title 반다라트 제목
 * @param profileEmoji 반다라트 프로필 이모지
 * @param isShared 공유 완료 여부
 * @param bandalartChartImageUri 반다라트 표 이미지 uri
 */
data class CompleteUiState(
    val id: Long = 0L,
    val title: String = "",
    val profileEmoji: String = "",
    val isShared: Boolean = false,
    val bandalartChartImageUri: String = "",
)
