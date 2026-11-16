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
import androidx.room.Index
import androidx.room.PrimaryKey

// 반다라트 삭제나 백업 복원 뒤에도 완료 이력을 남기기 위해 외래 키를 두지 않는다.
@Entity(
    tableName = "cell_completion_records",
    indices = [
        // 셀당 하루 1건만 기록
        Index(value = ["cellId", "completedDate"], unique = true),
        // 기간별 완료 기록 조회 최적화
        Index(value = ["completedDate"]),
    ],
)
data class CellCompletionRecordDBEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long? = null,
    @ColumnInfo(name = "bandalartId")
    val bandalartId: Long,
    @ColumnInfo(name = "cellId")
    val cellId: Long,
    @ColumnInfo(name = "completedDate")
    val completedDate: String,
    @ColumnInfo(name = "completedAt")
    val completedAt: Long,
)
