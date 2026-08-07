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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.emoji_picker_category_activities
import bandalart.core.designsystem.generated.resources.emoji_picker_category_all
import bandalart.core.designsystem.generated.resources.emoji_picker_category_flags
import bandalart.core.designsystem.generated.resources.emoji_picker_category_food
import bandalart.core.designsystem.generated.resources.emoji_picker_category_nature
import bandalart.core.designsystem.generated.resources.emoji_picker_category_objects
import bandalart.core.designsystem.generated.resources.emoji_picker_category_people
import bandalart.core.designsystem.generated.resources.emoji_picker_category_smileys
import bandalart.core.designsystem.generated.resources.emoji_picker_category_symbols
import bandalart.core.designsystem.generated.resources.emoji_picker_category_travel
import bandalart.core.designsystem.generated.resources.emoji_picker_close
import bandalart.core.designsystem.generated.resources.emoji_picker_empty
import bandalart.core.designsystem.generated.resources.emoji_picker_reset
import bandalart.core.designsystem.generated.resources.emoji_picker_search_placeholder
import bandalart.core.designsystem.generated.resources.emoji_picker_title
import com.nexters.bandalart.core.common.getLocale
import com.nexters.bandalart.core.ui.NavigationBarHeightDp
import org.jetbrains.compose.resources.stringResource

@Composable
fun FluentEmojiPicker(
    currentEmoji: String?,
    onEmojiSelect: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedEmoji by rememberSaveable(currentEmoji) { mutableStateOf(currentEmoji) }

    val selectedCategory = selectedCategoryName?.let { FluentEmojiCategory.valueOf(it) }
    val filteredItems =
        remember(query, selectedCategory) {
            filterFluentEmojiItems(
                query = query,
                category = selectedCategory,
            )
        }
    val sizeModifier =
        if (expanded) {
            Modifier.fillMaxHeight()
        } else {
            Modifier.heightIn(min = 360.dp, max = 480.dp)
        }

    Column(
        modifier =
            modifier
                .then(sizeModifier)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.emoji_picker_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = onClose) {
                Text(text = stringResource(Res.string.emoji_picker_close))
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = {
                Text(text = stringResource(Res.string.emoji_picker_search_placeholder))
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )

        EmojiCategoryRow(
            selectedCategory = selectedCategory,
            onCategorySelect = { category ->
                selectedCategoryName = category?.name
            },
        )

        if (filteredItems.isEmpty()) {
            EmojiPickerEmptyState(
                onReset = {
                    query = ""
                    selectedCategoryName = null
                },
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 56.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        top = 12.dp,
                        end = 16.dp,
                        bottom = if (expanded) NavigationBarHeightDp + 16.dp else 16.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = filteredItems,
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
        }
    }
}

@Composable
private fun EmojiCategoryRow(
    selectedCategory: FluentEmojiCategory?,
    onCategorySelect: (FluentEmojiCategory?) -> Unit,
) {
    val categories = remember { listOf(null) + FluentEmojiCategory.entries }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = categories,
            key = { it?.name ?: "all" },
        ) { category ->
            val selected = selectedCategory == category
            Surface(
                modifier =
                    Modifier.selectable(
                        selected = selected,
                        onClick = { onCategorySelect(category) },
                    ),
                shape = RoundedCornerShape(50),
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                    ),
            ) {
                Text(
                    text = categoryLabel(category),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
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
    Surface(
        modifier =
            Modifier
                .aspectRatio(1f)
                .selectable(
                    selected = selected,
                    onClick = onClick,
                ).semantics(mergeDescendants = true) {
                    this.contentDescription = contentDescription
                },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border =
            BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            BandalartEmoji(
                unicode = item.unicode,
                contentDescription = null,
                size = 32.dp,
            )
            if (selected) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clearAndSetSemantics { },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "✓",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 10.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmojiPickerEmptyState(
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.emoji_picker_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onReset) {
                Text(text = stringResource(Res.string.emoji_picker_reset))
            }
        }
    }
}

@Composable
private fun categoryLabel(category: FluentEmojiCategory?): String =
    stringResource(
        when (category) {
            null -> Res.string.emoji_picker_category_all
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
