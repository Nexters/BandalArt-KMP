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

package com.nexters.bandalart.core.data.mapper

import com.nexters.bandalart.core.database.entity.UpdateBandalartEmojiDto
import com.nexters.bandalart.core.database.entity.UpdateBandalartMainCellDto
import com.nexters.bandalart.core.database.entity.UpdateBandalartSubCellDto
import com.nexters.bandalart.core.database.entity.UpdateBandalartTaskCellDto
import com.nexters.bandalart.core.domain.entity.UpdateBandalartEmojiEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartMainCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartSubCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartTaskCellEntity

fun UpdateBandalartMainCellEntity.toDto(): UpdateBandalartMainCellDto =
    UpdateBandalartMainCellDto(
        title = title,
        description = description,
        dueDate = dueDate,
        profileEmoji = profileEmoji,
        mainColor = mainColor,
        subColor = subColor,
    )

fun UpdateBandalartSubCellEntity.toDto(): UpdateBandalartSubCellDto =
    UpdateBandalartSubCellDto(
        title = title,
        description = description,
        dueDate = dueDate,
    )

fun UpdateBandalartTaskCellEntity.toDto(): UpdateBandalartTaskCellDto =
    UpdateBandalartTaskCellDto(
        title = title,
        description = description,
        dueDate = dueDate,
        isCompleted = isCompleted,
    )

fun UpdateBandalartEmojiEntity.toDto(): UpdateBandalartEmojiDto =
    UpdateBandalartEmojiDto(
        profileEmoji = profileEmoji,
    )
