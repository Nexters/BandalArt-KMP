/*
 * Copyright 2025 easyhooon
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
