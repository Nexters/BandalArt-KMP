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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nexters.bandalart.core.common.getLocale

private const val PICKER_COLUMN_COUNT = 6
private const val PICKER_VISIBLE_ROW_COUNT = 4
private val PICKER_CELL_SPACING = 8.dp

@Composable
fun FluentEmojiPicker(
    currentEmoji: String?,
    recentEmojis: List<String>,
    onEmojiSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
) {
    var selectedEmoji by rememberSaveable(currentEmoji) { mutableStateOf(currentEmoji) }
    val pickerItems =
        remember(recentEmojis) {
            val recentItems =
                filterFluentEmojiItems(
                    query = "",
                    category = FluentEmojiCategory.RECENT,
                    recentEmojis = recentEmojis,
                )
            recentItems + FluentEmojiCatalog.items.filterNot { it in recentItems }
        }
    val horizontalPadding = if (expanded) 23.dp else 8.dp
    val topPadding = if (expanded) 23.dp else 8.dp
    val bottomPadding = if (expanded) 26.dp else 0.dp

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
                bottomPadding +
                cellSize * PICKER_VISIBLE_ROW_COUNT +
                PICKER_CELL_SPACING * (PICKER_VISIBLE_ROW_COUNT - 1)

        Column(modifier = Modifier.fillMaxWidth()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(PICKER_COLUMN_COUNT),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(gridHeight),
                contentPadding =
                    PaddingValues(
                        start = horizontalPadding,
                        top = topPadding,
                        end = horizontalPadding,
                        bottom = bottomPadding,
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
