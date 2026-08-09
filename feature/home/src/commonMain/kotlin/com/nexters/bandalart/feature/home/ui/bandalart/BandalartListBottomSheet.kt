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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.add_description
import bandalart.core.designsystem.generated.resources.bandalart_list_add
import bandalart.core.designsystem.generated.resources.bandalart_list_title
import bandalart.core.designsystem.generated.resources.clear_description
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
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
                Text(
                    text = stringResource(Res.string.bandalart_list_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W700,
                    modifier = modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                IconButton(
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .height(21.dp)
                            .aspectRatio(1f),
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
            Spacer(modifier = Modifier.height(40.dp))
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
                            // 앱에 진입할때 가장 최근에 확인한 표가 화면에 보여지도록
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
                                onHomeUiAction(HomeScreen.Event.AddBandalart)
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
            onHomeUiAction = {},
        )
    }
}
