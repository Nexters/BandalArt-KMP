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

package com.nexters.bandalart.feature.home.ui.bandalart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.add_description
import bandalart.core.designsystem.generated.resources.back_description
import bandalart.core.designsystem.generated.resources.bandalart_create_direct_summary
import bandalart.core.designsystem.generated.resources.bandalart_create_direct_title
import bandalart.core.designsystem.generated.resources.bandalart_create_template_request
import bandalart.core.designsystem.generated.resources.bandalart_create_template_section
import bandalart.core.designsystem.generated.resources.bandalart_create_title
import bandalart.core.designsystem.generated.resources.bandalart_list_add
import bandalart.core.designsystem.generated.resources.bandalart_list_title
import bandalart.core.designsystem.generated.resources.clear_description
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.domain.template.BandalartTemplate
import com.nexters.bandalart.core.domain.template.BandalartTemplateCatalog
import com.nexters.bandalart.feature.home.model.BandalartUiModel
import com.nexters.bandalart.feature.home.model.dummy.dummyBandalartList
import com.nexters.bandalart.feature.home.HomeScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandalartListBottomSheet(
    bandalartList: ImmutableList<BandalartUiModel>,
    currentBandalartId: Long,
    isCreationOptionsVisible: Boolean,
    onHomeUiAction: (HomeScreen.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            onHomeUiAction(HomeScreen.Event.DismissBottomSheet)
        },
        modifier =
            Modifier
                .wrapContentSize()
                .statusBarsPadding(),
        sheetState = bottomSheetState,
        dragHandle = null,
    ) {
        Column(
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.surface),
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
            ) {
                if (isCreationOptionsVisible) {
                    IconButton(
                        modifier =
                            Modifier
                                .align(Alignment.CenterStart)
                                .size(48.dp),
                        onClick = {
                            onHomeUiAction(HomeScreen.Event.CloseBandalartCreationOptions)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back_description),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Text(
                    text =
                        stringResource(
                            if (isCreationOptionsVisible) {
                                Res.string.bandalart_create_title
                            } else {
                                Res.string.bandalart_list_title
                            },
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W700,
                    modifier =
                        modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp),
                    textAlign = TextAlign.Center,
                )
                IconButton(
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .size(48.dp),
                    onClick = {
                        onHomeUiAction(HomeScreen.Event.DismissBottomSheet)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(Res.string.clear_description),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(modifier = Modifier.height(if (isCreationOptionsVisible) 24.dp else 40.dp))
            if (isCreationOptionsVisible) {
                BandalartCreationOptions(
                    onHomeUiAction = onHomeUiAction,
                )
            } else {
                BandalartList(
                    bandalartList = bandalartList,
                    currentBandalartId = currentBandalartId,
                    onHomeUiAction = onHomeUiAction,
                )
            }
        }
    }
}

@Composable
private fun BandalartList(
    bandalartList: ImmutableList<BandalartUiModel>,
    currentBandalartId: Long,
    onHomeUiAction: (HomeScreen.Event) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(
            count = bandalartList.size,
            key = { index -> bandalartList[index].id },
        ) { index ->
            val bandalartItem = bandalartList[index]
            BandalartListItem(
                bandalartItem = bandalartItem,
                currentBandalartId = currentBandalartId,
                onClick = { key ->
                    onHomeUiAction(HomeScreen.Event.SelectBandalart(key))
                },
            )
        }
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row {
                Button(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(56.dp)
                            .padding(horizontal = 24.dp),
                    onClick = {
                        onHomeUiAction(HomeScreen.Event.OpenBandalartCreationOptions)
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                ) {
                    Row {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(Res.string.add_description),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.padding(start = 4.dp))
                        Text(
                            text = stringResource(Res.string.bandalart_list_add),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W600,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BandalartCreationOptions(onHomeUiAction: (HomeScreen.Event) -> Unit) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            CreationOptionRow(
                icon = {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                title = stringResource(Res.string.bandalart_create_direct_title),
                summary = stringResource(Res.string.bandalart_create_direct_summary),
                onClick = { onHomeUiAction(HomeScreen.Event.AddBandalart) },
            )
        }
        item {
            Text(
                text = stringResource(Res.string.bandalart_create_template_section),
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W700,
            )
        }
        items(
            items = BandalartTemplateCatalog.templates,
            key = { template -> template.id.storedValue },
        ) { template ->
            TemplateOptionRow(
                template = template,
                onClick = {
                    onHomeUiAction(HomeScreen.Event.CreateBandalartFromTemplate(template.id))
                },
            )
        }
        item {
            TextButton(
                onClick = { onHomeUiAction(HomeScreen.Event.ContactSupport) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = stringResource(Res.string.bandalart_create_template_request))
            }
        }
    }
}

@Composable
private fun TemplateOptionRow(
    template: BandalartTemplate,
    onClick: () -> Unit,
) {
    CreationOptionRow(
        icon = {
            Text(
                text = template.profileEmoji,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
            )
        },
        title = template.title,
        summary = template.summary,
        onClick = onClick,
    )
}

@Composable
private fun CreationOptionRow(
    icon: @Composable () -> Unit,
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { role = Role.Button },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

// @ComponentPreview
@Preview
@Composable
private fun BandalartListBottomSheetPreview() {
    BandalartTheme {
        BandalartListBottomSheet(
            bandalartList = dummyBandalartList.toImmutableList(),
            currentBandalartId = 0L,
            isCreationOptionsVisible = false,
            onHomeUiAction = {},
        )
    }
}
