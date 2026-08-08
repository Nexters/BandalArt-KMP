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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.emoji_picker_category_activities
import bandalart.core.designsystem.generated.resources.emoji_picker_category_all
import bandalart.core.designsystem.generated.resources.emoji_picker_category_flags
import bandalart.core.designsystem.generated.resources.emoji_picker_category_food
import bandalart.core.designsystem.generated.resources.emoji_picker_category_nature
import bandalart.core.designsystem.generated.resources.emoji_picker_category_objects
import bandalart.core.designsystem.generated.resources.emoji_picker_category_people
import bandalart.core.designsystem.generated.resources.emoji_picker_category_recent
import bandalart.core.designsystem.generated.resources.emoji_picker_category_smileys
import bandalart.core.designsystem.generated.resources.emoji_picker_category_symbols
import bandalart.core.designsystem.generated.resources.emoji_picker_category_travel
import com.nexters.bandalart.core.common.getLocale
import org.jetbrains.compose.resources.stringResource

private const val PICKER_COLUMN_COUNT = 6
private const val PICKER_VISIBLE_ROW_COUNT = 4
private val PICKER_CELL_SPACING = 8.dp
private val CATEGORY_TAB_SPACING = 4.dp

internal data class FluentEmojiCategoryTab(
    val category: FluentEmojiCategory?,
    val iconUnicode: String,
)

private val fluentEmojiCategoryTabs =
    listOf(
        FluentEmojiCategoryTab(category = null, iconUnicode = "✨"),
        FluentEmojiCategoryTab(category = FluentEmojiCategory.RECENT, iconUnicode = "⏰"),
        FluentEmojiCategoryTab(
            category = FluentEmojiCategory.SMILEYS_AND_EMOTION,
            iconUnicode = "😍",
        ),
        FluentEmojiCategoryTab(category = FluentEmojiCategory.PEOPLE_AND_BODY, iconUnicode = "💪"),
        FluentEmojiCategoryTab(category = FluentEmojiCategory.ANIMALS_AND_NATURE, iconUnicode = "🐻"),
        FluentEmojiCategoryTab(category = FluentEmojiCategory.FOOD_AND_DRINK, iconUnicode = "🍉"),
        FluentEmojiCategoryTab(category = FluentEmojiCategory.TRAVEL_AND_PLACES, iconUnicode = "🚀"),
        FluentEmojiCategoryTab(category = FluentEmojiCategory.ACTIVITIES, iconUnicode = "🎯"),
        FluentEmojiCategoryTab(category = FluentEmojiCategory.OBJECTS, iconUnicode = "📚"),
        FluentEmojiCategoryTab(category = FluentEmojiCategory.SYMBOLS, iconUnicode = "✔️"),
        FluentEmojiCategoryTab(category = FluentEmojiCategory.FLAGS, iconUnicode = "🏁"),
    )

internal fun visibleFluentEmojiCategoryTabs(hasRecentEmojis: Boolean): List<FluentEmojiCategoryTab> =
    if (hasRecentEmojis) {
        fluentEmojiCategoryTabs
    } else {
        fluentEmojiCategoryTabs.filterNot { it.category == FluentEmojiCategory.RECENT }
    }

internal fun fluentEmojiPickerItems(
    category: FluentEmojiCategory?,
    recentEmojis: List<String>,
): List<FluentEmojiItem> {
    if (category != null && category != FluentEmojiCategory.RECENT) {
        return filterFluentEmojiItems(query = "", category = category)
    }

    val recentItems =
        filterFluentEmojiItems(
            query = "",
            category = FluentEmojiCategory.RECENT,
            recentEmojis = recentEmojis,
        )
    if (category == FluentEmojiCategory.RECENT) {
        return recentItems
    }

    val recentUnicode = recentItems.mapTo(mutableSetOf(), FluentEmojiItem::unicode)
    return recentItems + FluentEmojiCatalog.items.filterNot { it.unicode in recentUnicode }
}

@Composable
fun FluentEmojiPicker(
    currentEmoji: String?,
    recentEmojis: List<String>,
    onEmojiSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
) {
    var selectedCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedEmoji by rememberSaveable(currentEmoji) { mutableStateOf(currentEmoji) }
    val hasRecentEmojis =
        remember(recentEmojis) {
            fluentEmojiPickerItems(
                category = FluentEmojiCategory.RECENT,
                recentEmojis = recentEmojis,
            ).isNotEmpty()
        }
    val selectedCategory =
        selectedCategoryName
            ?.let { savedName ->
                FluentEmojiCategory.entries.firstOrNull { it.name == savedName }
            }?.takeUnless { it == FluentEmojiCategory.RECENT && !hasRecentEmojis }
    val pickerItems =
        remember(selectedCategory, recentEmojis) {
            fluentEmojiPickerItems(
                category = selectedCategory,
                recentEmojis = recentEmojis,
            )
        }
    val gridState = rememberLazyGridState()
    val previousHasRecentEmojis = remember { mutableStateOf(hasRecentEmojis) }
    val horizontalPadding = if (expanded) 23.dp else 8.dp
    val topPadding = if (expanded) 23.dp else 8.dp
    val bottomPadding = if (expanded) 26.dp else 0.dp

    LaunchedEffect(hasRecentEmojis) {
        val transitionedToEmpty = previousHasRecentEmojis.value && !hasRecentEmojis
        previousHasRecentEmojis.value = hasRecentEmojis
        if (
            transitionedToEmpty &&
            selectedCategoryName == FluentEmojiCategory.RECENT.name
        ) {
            selectedCategoryName = null
        }
    }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = if (expanded) 16.dp else 0.dp),
    ) {
        val cellSize =
            (maxWidth -
                horizontalPadding * 2 -
                PICKER_CELL_SPACING * (PICKER_COLUMN_COUNT - 1)) / PICKER_COLUMN_COUNT
        val gridHeight =
            topPadding +
                cellSize * PICKER_VISIBLE_ROW_COUNT +
                PICKER_CELL_SPACING * (PICKER_VISIBLE_ROW_COUNT - 1)

        Column(modifier = Modifier.fillMaxWidth()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(PICKER_COLUMN_COUNT),
                state = gridState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(gridHeight),
                contentPadding =
                    PaddingValues(
                        start = horizontalPadding,
                        top = topPadding,
                        end = horizontalPadding,
                        bottom = 0.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(PICKER_CELL_SPACING),
                verticalArrangement = Arrangement.spacedBy(PICKER_CELL_SPACING),
            ) {
                items(
                    items = pickerItems,
                    key = FluentEmojiItem::unicode,
                ) { item ->
                    FluentEmojiPickerCell(
                        item = item,
                        selected = selectedEmoji == item.unicode,
                        onClick = {
                            if (selectedEmoji != item.unicode) {
                                selectedEmoji = item.unicode
                                onEmojiSelect(item.unicode)
                            }
                        },
                    )
                }
            }
            EmojiCategoryRow(
                selectedCategory = selectedCategory,
                hasRecentEmojis = hasRecentEmojis,
                onCategorySelect = { category ->
                    if (selectedCategory != category) {
                        gridState.requestScrollToItem(0)
                        selectedCategoryName = category?.name
                    }
                },
                modifier = Modifier.padding(bottom = bottomPadding),
            )
        }
    }
}

@Composable
private fun EmojiCategoryRow(
    selectedCategory: FluentEmojiCategory?,
    hasRecentEmojis: Boolean,
    onCategorySelect: (FluentEmojiCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = remember(hasRecentEmojis) { visibleFluentEmojiCategoryTabs(hasRecentEmojis) }

    LazyRow(
        modifier =
            modifier
                .fillMaxWidth()
                .selectableGroup(),
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(CATEGORY_TAB_SPACING),
    ) {
        items(
            items = tabs,
            key = { it.category?.name ?: "all" },
        ) { tab ->
            val selected = selectedCategory == tab.category
            val label = categoryLabel(tab.category)

            Surface(
                modifier =
                    Modifier
                        .size(48.dp)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onCategorySelect(tab.category) },
                        ).semantics(mergeDescendants = true) {
                            contentDescription = label
                        },
                shape = RoundedCornerShape(12.dp),
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                border =
                    if (selected) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null
                    },
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    BandalartEmoji(
                        unicode = tab.iconUnicode,
                        contentDescription = null,
                        size = 24.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun FluentEmojiPickerCell(
    item: FluentEmojiItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val contentDescription = item.displayName(getLocale().language)

    Card(
        modifier = Modifier.aspectRatio(1f),
        shape = RoundedCornerShape(12.dp),
        border =
            if (selected) {
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                null
            },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .selectable(
                        selected = selected,
                        onClick = onClick,
                    ).semantics(mergeDescendants = true) {
                        this.contentDescription = contentDescription
                    },
            contentAlignment = Alignment.Center,
        ) {
            BandalartEmoji(
                unicode = item.unicode,
                contentDescription = null,
                size = 24.dp,
            )
        }
    }
}

@Composable
private fun categoryLabel(category: FluentEmojiCategory?): String =
    stringResource(
        when (category) {
            null -> Res.string.emoji_picker_category_all
            FluentEmojiCategory.RECENT -> Res.string.emoji_picker_category_recent
            FluentEmojiCategory.SMILEYS_AND_EMOTION -> Res.string.emoji_picker_category_smileys
            FluentEmojiCategory.PEOPLE_AND_BODY -> Res.string.emoji_picker_category_people
            FluentEmojiCategory.ANIMALS_AND_NATURE -> Res.string.emoji_picker_category_nature
            FluentEmojiCategory.FOOD_AND_DRINK -> Res.string.emoji_picker_category_food
            FluentEmojiCategory.TRAVEL_AND_PLACES -> Res.string.emoji_picker_category_travel
            FluentEmojiCategory.ACTIVITIES -> Res.string.emoji_picker_category_activities
            FluentEmojiCategory.OBJECTS -> Res.string.emoji_picker_category_objects
            FluentEmojiCategory.SYMBOLS -> Res.string.emoji_picker_category_symbols
            FluentEmojiCategory.FLAGS -> Res.string.emoji_picker_category_flags
        },
    )
