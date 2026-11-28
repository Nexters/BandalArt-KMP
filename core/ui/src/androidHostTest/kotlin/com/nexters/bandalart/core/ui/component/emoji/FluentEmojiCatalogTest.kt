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
import bandalart.core.designsystem.generated.resources.emoji_category_nav_activities
import bandalart.core.designsystem.generated.resources.emoji_category_nav_all
import bandalart.core.designsystem.generated.resources.emoji_category_nav_flags
import bandalart.core.designsystem.generated.resources.emoji_category_nav_food
import bandalart.core.designsystem.generated.resources.emoji_category_nav_nature
import bandalart.core.designsystem.generated.resources.emoji_category_nav_objects
import bandalart.core.designsystem.generated.resources.emoji_category_nav_people
import bandalart.core.designsystem.generated.resources.emoji_category_nav_recent
import bandalart.core.designsystem.generated.resources.emoji_category_nav_smileys
import bandalart.core.designsystem.generated.resources.emoji_category_nav_symbols
import bandalart.core.designsystem.generated.resources.emoji_category_nav_travel
import com.nexters.bandalart.core.common.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class FluentEmojiCatalogTest {
    @Test
    fun finalizedCatalogContainsEveryFluentCandidate() {
        assertEquals(CATALOG_SIZE, catalogItems.size)
    }

    @Test
    fun registeredUnicodeMapsToStableResourceKey() {
        assertEquals("fluent_1f3af", fluentEmojiResourceKey("🎯"))
        assertEquals("fluent_2764_fe0f_200d_1f525", fluentEmojiResourceKey("❤️‍🔥"))
        assertEquals("files/fluent_emoji/fluent_1f3af.webp", fluentEmojiImagePath("🎯"))
    }

    @Test
    fun unregisteredLegacyUnicodeIsNotClaimedByFluentCatalog() {
        listOf("👩‍💻", "👨‍⚕️").forEach { unicode ->
            assertTrue(catalogItems.none { it.unicode == unicode })
            assertFalse(composeResourceFile(fluentEmojiImagePath(unicode)).exists())
        }
    }

    @Test
    fun missingImageFallsBackToSystemEmojiInsteadOfThrowing() {
        // Android's resource reader throws for a missing file; this runs before the catalog is read.
        val uri =
            fluentEmojiImageUri("👩‍💻") { path ->
                throw IllegalStateException("Missing resource with path: $path")
            }

        assertNull(uri)
        assertEquals("uri:files/fluent_emoji/fluent_1f3af.webp", fluentEmojiImageUri("🎯") { "uri:$it" })
    }

    @Test
    fun everyCatalogItemHasUniqueUnicodeAndKnownCategory() {
        assertEquals(
            CATALOG_SIZE,
            catalogItems
                .map { it.unicode }
                .distinct()
                .size,
        )
        assertEquals(
            FluentEmojiCategory.entries.filterNot { it == FluentEmojiCategory.RECENT }.toSet(),
            catalogItems.map { it.category }.toSet(),
        )
        assertTrue(
            catalogItems.all { item -> composeResourceFile(fluentEmojiImagePath(item.unicode)).isFile },
        )
        assertEquals(
            CATALOG_SIZE,
            composeResourceFile("files/fluent_emoji").listFiles { file -> file.extension == "webp" }?.size,
        )
    }

    @Test
    fun searchMatchesEnglishNameKeywordAndKoreanAlias() {
        assertTrue(filterFluentEmojiItems(catalogItems, "bullseye", null).any { it.unicode == "🎯" })
        assertTrue(filterFluentEmojiItems(catalogItems, "dart", null).any { it.unicode == "🎯" })
        assertTrue(filterFluentEmojiItems(catalogItems, "저축", null).any { it.unicode == "🪙" })
    }

    @Test
    fun categoryAndQueryFiltersCanBeCombined() {
        val activities = filterFluentEmojiItems(catalogItems, "", FluentEmojiCategory.ACTIVITIES)

        assertTrue(activities.isNotEmpty())
        assertTrue(activities.all { it.category == FluentEmojiCategory.ACTIVITIES })
        val targetResults = filterFluentEmojiItems(catalogItems, "target", FluentEmojiCategory.ACTIVITIES)
        assertTrue(targetResults.any { it.unicode == "🎯" })
        assertTrue(targetResults.all { it.category == FluentEmojiCategory.ACTIVITIES })
        assertTrue(filterFluentEmojiItems(catalogItems, "없는 검색어", null).isEmpty())
    }

    @Test
    fun blankQueryReturnsTheWholeCatalogAndKoreanNameUsesCuratedAlias() {
        assertEquals(CATALOG_SIZE, filterFluentEmojiItems(catalogItems, "   ", null).size)

        val target = catalogItems.single { it.unicode == "🎯" }
        assertEquals("목표", target.displayName(Language.KOREAN))
        assertEquals("bullseye", target.displayName(Language.ENGLISH))
        assertEquals("的", target.displayName(Language.JAPANESE))

        val uncategorizedAlias = catalogItems.first { it.koreanAliases.isEmpty() }
        assertEquals(uncategorizedAlias.koreanName, uncategorizedAlias.displayName(Language.KOREAN))
    }

    @Test
    fun searchMatchesCldrKoreanAndJapaneseAnnotations() {
        assertTrue(filterFluentEmojiItems(catalogItems, "고양이", null).any { it.unicode == "🐱" })
        assertTrue(filterFluentEmojiItems(catalogItems, "猫", null).any { it.unicode == "🐱" })
        assertTrue(filterFluentEmojiItems(catalogItems, "불타는 사랑", null).any { it.unicode == "❤️‍🔥" })
        assertTrue(filterFluentEmojiItems(catalogItems, "燃えるハート", null).any { it.unicode == "❤️‍🔥" })
    }

    @Test
    fun representativeDogFaceIsSearchableInEveryLanguage() {
        assertTrue(filterFluentEmojiItems(catalogItems, "강아지", null).any { it.unicode == "🐶" })
        assertTrue(filterFluentEmojiItems(catalogItems, "犬", null).any { it.unicode == "🐶" })
        assertTrue(filterFluentEmojiItems(catalogItems, "dog", null).any { it.unicode == "🐶" })
    }

    @Test
    fun everyCatalogItemHasKoreanAndJapaneseSearchData() {
        assertTrue(
            catalogItems.all { item ->
                item.koreanName.isNotBlank() &&
                    item.koreanKeywords.isNotEmpty() &&
                    item.japaneseName.isNotBlank() &&
                    item.japaneseKeywords.isNotEmpty()
            },
        )
    }

    @Test
    fun recentFilterKeepsStoredOrderAndDropsItemsOutsideTheCatalog() {
        val recentItems =
            filterFluentEmojiItems(
                items = catalogItems,
                query = "",
                category = FluentEmojiCategory.RECENT,
                recentEmojis = listOf("🚀", "👩‍💻", "🎯", "🚀"),
            )

        assertEquals(listOf("🚀", "🎯"), recentItems.map { it.unicode })
        assertEquals(
            listOf("🎯"),
            filterFluentEmojiItems(
                items = catalogItems,
                query = "target",
                category = FluentEmojiCategory.RECENT,
                recentEmojis = listOf("🚀", "🎯"),
            ).map { it.unicode },
        )
    }

    @Test
    fun allPickerViewKeepsRecentItemsFirstWithoutDuplicatingCatalogItems() {
        val items =
            fluentEmojiPickerItems(
                items = catalogItems,
                category = null,
                recentEmojis = listOf("🚀", "👩‍💻", "🎯", "🚀"),
            )

        assertEquals(listOf("🚀", "🎯"), items.take(2).map { it.unicode })
        assertEquals(CATALOG_SIZE, items.size)
        assertEquals(CATALOG_SIZE, items.map { it.unicode }.distinct().size)
    }

    @Test
    fun pickerCategoryViewContainsOnlyTheSelectedMetadataGroup() {
        FluentEmojiCategory.entries
            .filterNot { it == FluentEmojiCategory.RECENT }
            .forEach { category ->
                val items =
                    fluentEmojiPickerItems(
                        items = catalogItems,
                        category = category,
                        recentEmojis = emptyList(),
                    )

                assertTrue(items.isNotEmpty())
                assertTrue(items.all { it.category == category })
            }
    }

    @Test
    fun recentCategoryTabIsOnlyVisibleWhenCatalogBackedRecentItemsExist() {
        assertTrue(
            visibleFluentEmojiCategoryTabs(hasRecentEmojis = false)
                .none { it.category == FluentEmojiCategory.RECENT },
        )
        assertTrue(
            visibleFluentEmojiCategoryTabs(hasRecentEmojis = true)
                .any { it.category == FluentEmojiCategory.RECENT },
        )
        assertEquals(
            listOf(null) + FluentEmojiCategory.entries,
            visibleFluentEmojiCategoryTabs(hasRecentEmojis = true).map { it.category },
        )
    }

    @Test
    fun everyCategoryTabUsesItsOwnNavigationVectorResource() {
        val tabs = visibleFluentEmojiCategoryTabs(hasRecentEmojis = true)
        val expectedResources =
            listOf(
                Res.drawable.emoji_category_nav_all,
                Res.drawable.emoji_category_nav_recent,
                Res.drawable.emoji_category_nav_smileys,
                Res.drawable.emoji_category_nav_people,
                Res.drawable.emoji_category_nav_nature,
                Res.drawable.emoji_category_nav_food,
                Res.drawable.emoji_category_nav_travel,
                Res.drawable.emoji_category_nav_activities,
                Res.drawable.emoji_category_nav_objects,
                Res.drawable.emoji_category_nav_symbols,
                Res.drawable.emoji_category_nav_flags,
            )

        assertEquals(expectedResources, tabs.map { it.iconResource })
        assertEquals(
            tabs.size,
            tabs.map { it.iconResource }.distinct().size,
        )
    }

    private companion object {
        const val CATALOG_SIZE = 1428

        // Gradle runs host tests from the module directory.
        fun composeResourceFile(path: String) = File("../designsystem/src/commonMain/composeResources/$path")

        val catalogItems = parseFluentEmojiCatalog(composeResourceFile("files/fluent_emoji_catalog.json").readText())
    }
}
