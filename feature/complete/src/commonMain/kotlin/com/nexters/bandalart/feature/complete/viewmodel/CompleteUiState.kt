package com.nexters.bandalart.feature.complete.viewmodel

/**
 * CompleteUiState
 *
 * @param id 반다라트 고유 id
 * @param title 반다라트 제목
 * @param profileEmoji 반다라트 프로필 이모지
 * @param shareUrl 공유 링크
 */
data class CompleteUiState(
    val id: Long = 0L,
    val title: String = "",
    val profileEmoji: String = "",
    val isShared: Boolean = false,
    val bandalartChartImageUri: String = "",
)
