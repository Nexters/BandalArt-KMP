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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FluentEmojiCatalogTest {
    @Test
    fun finalizedCatalogContainsThreeHundredItems() {
        assertEquals(300, FluentEmojiCatalog.size)
    }

    @Test
    fun registeredUnicodeMapsToStableResourceKey() {
        assertEquals("fluent_1f3af", FluentEmojiCatalog.resourceKeyFor("🎯"))
        assertEquals(
            "fluent_2764_fe0f_200d_1f525",
            FluentEmojiCatalog.resourceKeyFor("❤️‍🔥"),
        )
    }

    @Test
    fun unregisteredLegacyUnicodeIsNotClaimedByFluentCatalog() {
        assertNull(FluentEmojiCatalog.resourceKeyFor("😎"))
        assertNull(FluentEmojiCatalog.resourceKeyFor("👨‍⚕️"))
    }

    @Test
    fun everyCatalogItemHasUniqueUnicodeAndKnownCategory() {
        assertEquals(
            300,
            FluentEmojiCatalog.items
                .map { it.unicode }
                .distinct()
                .size,
        )
        assertEquals(
            FluentEmojiCategory.entries.filterNot { it == FluentEmojiCategory.RECENT }.toSet(),
            FluentEmojiCatalog.items.map { it.category }.toSet(),
        )
        assertTrue(
            FluentEmojiCatalog.items.all { item ->
                FluentEmojiCatalog.resourceKeyFor(item.unicode) == item.resourceKey
            },
        )
    }

    @Test
    fun searchMatchesEnglishNameKeywordAndKoreanAlias() {
        assertTrue(filterFluentEmojiItems("bullseye", null).any { it.unicode == "🎯" })
        assertTrue(filterFluentEmojiItems("dart", null).any { it.unicode == "🎯" })
        assertTrue(filterFluentEmojiItems("저축", null).any { it.unicode == "🪙" })
    }

    @Test
    fun categoryAndQueryFiltersCanBeCombined() {
        val activities = filterFluentEmojiItems("", FluentEmojiCategory.ACTIVITIES)

        assertTrue(activities.isNotEmpty())
        assertTrue(activities.all { it.category == FluentEmojiCategory.ACTIVITIES })
        val targetResults = filterFluentEmojiItems("target", FluentEmojiCategory.ACTIVITIES)
        assertTrue(targetResults.any { it.unicode == "🎯" })
        assertTrue(targetResults.all { it.category == FluentEmojiCategory.ACTIVITIES })
        assertTrue(filterFluentEmojiItems("없는 검색어", null).isEmpty())
    }

    @Test
    fun blankQueryReturnsTheWholeCatalogAndKoreanNameUsesCuratedAlias() {
        assertEquals(300, filterFluentEmojiItems("   ", null).size)

        val target = FluentEmojiCatalog.items.single { it.unicode == "🎯" }
        assertEquals("목표", target.displayName(Language.KOREAN))
        assertEquals("bullseye", target.displayName(Language.ENGLISH))
        assertEquals("🎯", target.displayName(Language.JAPANESE))

        val uncategorizedAlias = FluentEmojiCatalog.items.first { it.koreanAliases.isEmpty() }
        assertEquals(uncategorizedAlias.unicode, uncategorizedAlias.displayName(Language.KOREAN))
    }

    @Test
    fun recentFilterKeepsStoredOrderAndDropsItemsOutsideTheCatalog() {
        val recentItems =
            filterFluentEmojiItems(
                query = "",
                category = FluentEmojiCategory.RECENT,
                recentEmojis = listOf("🚀", "😎", "🎯", "🚀"),
            )

        assertEquals(listOf("🚀", "🎯"), recentItems.map { it.unicode })
        assertEquals(
            listOf("🎯"),
            filterFluentEmojiItems(
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
                category = null,
                recentEmojis = listOf("🚀", "😎", "🎯", "🚀"),
            )

        assertEquals(listOf("🚀", "🎯"), items.take(2).map { it.unicode })
        assertEquals(300, items.size)
        assertEquals(300, items.map { it.unicode }.distinct().size)
    }

    @Test
    fun pickerCategoryViewContainsOnlyTheSelectedMetadataGroup() {
        FluentEmojiCategory.entries
            .filterNot { it == FluentEmojiCategory.RECENT }
            .forEach { category ->
                val items = fluentEmojiPickerItems(category = category, recentEmojis = emptyList())

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
}
