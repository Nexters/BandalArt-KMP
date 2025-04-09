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

package com.nexters.bandalart.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 반다라트 셀과 그의 하위 셀들의 관계를 표현하는 데이터 클래스
 * 1:N (일대다) 관계를 표현
 */
data class BandalartCellWithChildrenDto(
    // @Embedded: 해당 객체의 모든 필드를 현재 테이블의 컬럼으로 포함
    @Embedded
    val cell: BandalartCellDBEntity,
    // @Relation: 다른 엔터티와의 관계를 정의
    // parentColumn: 현재 엔터티(cell)의 기준이 되는 컬럼
    // entityColumn: 자식 엔터티에서 참조하는 컬럼
    @Relation(
        // BandalartCellDBEntity의 id
        parentColumn = "id",
        // 자식 셀들의 parentId
        entityColumn = "parentId",
    )
    val children: List<BandalartCellDBEntity>,
)

@Serializable
data class UpdateBandalartMainCellDto(
    @SerialName("title")
    val title: String?,
    @SerialName("description")
    val description: String?,
    @SerialName("dueDate")
    val dueDate: String?,
    @SerialName("profileEmoji")
    val profileEmoji: String?,
    @SerialName("mainColor")
    val mainColor: String,
    @SerialName("subColor")
    val subColor: String,
)

@Serializable
data class UpdateBandalartSubCellDto(
    @SerialName("title")
    val title: String?,
    @SerialName("description")
    val description: String?,
    @SerialName("dueDate")
    val dueDate: String?,
)

@Serializable
data class UpdateBandalartTaskCellDto(
    @SerialName("title")
    val title: String?,
    @SerialName("description")
    val description: String?,
    @SerialName("dueDate")
    val dueDate: String?,
    @SerialName("isCompleted")
    val isCompleted: Boolean? = null,
)

@Serializable
data class UpdateBandalartEmojiDto(
    @SerialName("profileEmoji")
    val profileEmoji: String?,
)
