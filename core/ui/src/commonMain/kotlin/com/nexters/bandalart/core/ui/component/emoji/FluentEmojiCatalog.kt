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

import bandalart.core.designsystem.generated.resources.Res
import com.nexters.bandalart.core.common.Language
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.concurrent.Volatile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

private const val CATALOG_RESOURCE_PATH = "files/fluent_emoji_catalog.json"

internal enum class FluentEmojiCategory {
    RECENT,
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

@Serializable
internal data class FluentEmojiItem(
    @SerialName("u") val unicode: String,
    @SerialName("c") val category: FluentEmojiCategory,
    @SerialName("n") val cldrName: String,
    @SerialName("k") val keywords: List<String>,
    @SerialName("a") val koreanAliases: List<String> = emptyList(),
    @SerialName("kn") val koreanName: String,
    @SerialName("kk") val koreanKeywords: List<String>,
    @SerialName("jn") val japaneseName: String,
    @SerialName("jk") val japaneseKeywords: List<String>,
) {
    val resourceKey: String
        get() = fluentEmojiResourceKey(unicode)

    fun displayName(language: Language): String =
        when (language) {
            Language.KOREAN -> koreanAliases.firstOrNull() ?: koreanName
            Language.ENGLISH -> cldrName
            Language.JAPANESE -> japaneseName
        }
}

internal object FluentEmojiCatalog {
    private val loadLock = Mutex()

    @Volatile
    private var loadedItems: List<FluentEmojiItem>? = null

    @Volatile
    private var loadedUnicodes: Set<String>? = null

    @OptIn(ExperimentalResourceApi::class)
    suspend fun items(): List<FluentEmojiItem> =
        loadLock.withLock {
            loadedItems ?: withContext(Dispatchers.Default) {
                parseFluentEmojiCatalog(Res.readBytes(CATALOG_RESOURCE_PATH).decodeToString())
            }.also { items ->
                loadedUnicodes = items.mapTo(HashSet(), FluentEmojiItem::unicode)
                loadedItems = items
            }
        }

    /** Null until the catalog has been read, so callers can tell "unknown" from "not in the catalog". */
    fun containsIfLoaded(unicode: String): Boolean? = loadedUnicodes?.contains(unicode)
}

/**
 * Reads the emoji picker catalog in the background so the picker usually opens with it ready.
 * A failure is ignored here; the catalog is not cached, so the picker reads it again when it opens.
 */
suspend fun preloadFluentEmojiCatalog() {
    runCatching { FluentEmojiCatalog.items() }
        .onFailure { if (it is CancellationException) throw it }
}

internal fun fluentEmojiImagePath(unicode: String): String = "files/fluent_emoji/${fluentEmojiResourceKey(unicode)}.webp"

internal fun parseFluentEmojiCatalog(json: String): List<FluentEmojiItem> = Json.decodeFromString(json)

// Drawable names are "fluent_" followed by each code point as lowercase hex, at least 4 digits.
internal fun fluentEmojiResourceKey(unicode: String): String =
    buildString {
        append("fluent")
        var index = 0
        while (index < unicode.length) {
            val char = unicode[index]
            val next = unicode.getOrNull(index + 1)
            val codePoint =
                if (char.isHighSurrogate() && next != null && next.isLowSurrogate()) {
                    index += 2
                    ((char.code - 0xD800) shl 10) + (next.code - 0xDC00) + 0x10000
                } else {
                    index += 1
                    char.code
                }
            append('_')
            append(codePoint.toString(16).padStart(4, '0'))
        }
    }

internal fun filterFluentEmojiItems(
    items: List<FluentEmojiItem>,
    query: String,
    category: FluentEmojiCategory?,
    recentEmojis: List<String> = emptyList(),
): List<FluentEmojiItem> {
    val normalizedQuery = query.trim().lowercase()
    val candidates =
        if (category == FluentEmojiCategory.RECENT) {
            val itemByUnicode = items.associateBy(FluentEmojiItem::unicode)
            recentEmojis.distinct().mapNotNull(itemByUnicode::get)
        } else {
            items
        }
    return candidates.filter { item ->
        val matchesCategory =
            category == null ||
                category == FluentEmojiCategory.RECENT ||
                item.category == category
        val matchesQuery =
            normalizedQuery.isEmpty() ||
                item.unicode.contains(normalizedQuery) ||
                item.cldrName.lowercase().contains(normalizedQuery) ||
                item.keywords.any { it.lowercase().contains(normalizedQuery) } ||
                item.koreanAliases.any { it.lowercase().contains(normalizedQuery) } ||
                item.koreanName.contains(normalizedQuery) ||
                item.koreanKeywords.any { it.contains(normalizedQuery) } ||
                item.japaneseName.contains(normalizedQuery) ||
                item.japaneseKeywords.any { it.contains(normalizedQuery) }
        matchesCategory && matchesQuery
    }
}
