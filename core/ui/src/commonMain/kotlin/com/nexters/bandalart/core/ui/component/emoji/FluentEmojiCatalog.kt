/*
 * Copyright 2026 easyhooon
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

package com.nexters.bandalart.core.ui.component.emoji

import com.nexters.bandalart.core.common.Language

internal enum class FluentEmojiCategory {
    SMILEYS_AND_EMOTION,
    PEOPLE_AND_BODY,
    ANIMALS_AND_NATURE,
    FOOD_AND_DRINK,
    TRAVEL_AND_PLACES,
    ACTIVITIES,
    OBJECTS,
    SYMBOLS,
    FLAGS,
}

internal data class FluentEmojiItem(
    val unicode: String,
    val resourceKey: String,
    val category: FluentEmojiCategory,
    val cldrName: String,
    val keywords: List<String>,
    val koreanAliases: List<String>,
) {
    fun displayName(language: Language): String =
        when (language) {
            Language.KOREAN -> koreanAliases.firstOrNull() ?: unicode
            Language.ENGLISH -> cldrName
            Language.JAPANESE -> unicode
        }
}

internal fun filterFluentEmojiItems(
    query: String,
    category: FluentEmojiCategory?,
): List<FluentEmojiItem> {
    val normalizedQuery = query.trim().lowercase()
    return FluentEmojiCatalog.items.filter { item ->
        val matchesCategory = category == null || item.category == category
        val matchesQuery =
            normalizedQuery.isEmpty() ||
                item.unicode.contains(normalizedQuery) ||
                item.cldrName.lowercase().contains(normalizedQuery) ||
                item.keywords.any { it.lowercase().contains(normalizedQuery) } ||
                item.koreanAliases.any { it.lowercase().contains(normalizedQuery) }
        matchesCategory && matchesQuery
    }
}
