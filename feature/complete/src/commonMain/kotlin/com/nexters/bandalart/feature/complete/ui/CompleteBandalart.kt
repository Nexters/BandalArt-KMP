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

package com.nexters.bandalart.feature.complete.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.complete_chart
import bandalart.core.designsystem.generated.resources.empty_emoji_description
import bandalart.core.designsystem.generated.resources.home_default_emoji
import bandalart.core.designsystem.generated.resources.ic_empty_emoji
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.designsystem.theme.Gray100
import com.nexters.bandalart.core.designsystem.theme.Gray300
import com.nexters.bandalart.core.designsystem.theme.Gray400
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CompleteBandalart(
    profileEmoji: String,
    title: String,
    // bandalartChartImageUri: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.complete_chart),
            color = Gray400,
            fontWeight = FontWeight.W600,
            fontSize = 14.sp,
            letterSpacing = (-0.28).sp,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 33.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 2.dp,
                    color = Gray300,
                    shape = RoundedCornerShape(12.dp),
                ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .width(52.dp)
                                .aspectRatio(1f)
                                .background(Gray100),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (profileEmoji == stringResource(Res.string.home_default_emoji)) {
                                Icon(
                                    imageVector = vectorResource(Res.drawable.ic_empty_emoji),
                                    contentDescription = stringResource(Res.string.empty_emoji_description),
                                    tint = Color.Unspecified,
                                )
                            } else {
                                Text(
                                    text = profileEmoji,
                                    fontSize = 22.sp,
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title,
                    color = Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W700,
                    letterSpacing = (-0.32).sp,
                )
                Spacer(modifier = Modifier.height(16.dp))
//                NetworkImage(
//                    imageUri = bandalartChartImageUri,
//                    contentDescription = "Complete Bandalart Chart Image",
//                    modifier = Modifier
//                        .width(328.dp)
//                        .height(328.dp),
//                )
//                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// @ComponentPreview
@Preview
@Composable
private fun CompleteBandalartPreview() {
    BandalartTheme {
        CompleteBandalart(
            profileEmoji = "😎",
            title = "발전하는 예진",
            // bandalartChartImageUri = "",
        )
    }
}
