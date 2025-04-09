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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bandalart_cells",
    // 데이터 무결성을 보장하기 위한 외래 키 제약 조건 설정
    foreignKeys = [
        ForeignKey(
            // 참조할 부모 엔터티 클래스
            entity = BandalartDBEntity::class,
            // 부모 테이블의 참조할 컬럼명
            parentColumns = ["id"],
            // 현재 테이블의 외래 키 컬럼명
            childColumns = ["bandalartId"],
            // 부모 레코드 삭제 시 자식 레코드도 함께 삭제
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    // 인덱스 설정 - 쿼리 성능 최적화를 위한 설정
    indices = [
        // 계층구조 조회 쿼리 최적화를 위한 parentId 인덱스
        Index(value = ["parentId"]),
        // 특정 반다라트에 속한 셀 조회 최적화를 위한 인덱스
        Index(value = ["bandalartId"]),
    ],
)
data class BandalartCellDBEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long? = null,
    @ColumnInfo(name = "bandalartId")
    val bandalartId: Long,
    @ColumnInfo(name = "title")
    val title: String? = null,
    @ColumnInfo(name = "description")
    val description: String? = null,
    @ColumnInfo(name = "dueDate")
    val dueDate: String? = null,
    @ColumnInfo(name = "isCompleted")
    val isCompleted: Boolean = false,
    @ColumnInfo(name = "parentId")
    val parentId: Long? = null,
)
