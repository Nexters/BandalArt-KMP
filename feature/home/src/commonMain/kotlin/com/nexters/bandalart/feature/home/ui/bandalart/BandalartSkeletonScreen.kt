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

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.bandalart
import bandalart.core.designsystem.generated.resources.edit_description
import bandalart.core.designsystem.generated.resources.empty_emoji_description
import bandalart.core.designsystem.generated.resources.home_layout_id
import bandalart.core.designsystem.generated.resources.home_main_id
import bandalart.core.designsystem.generated.resources.home_share
import bandalart.core.designsystem.generated.resources.ic_edit
import bandalart.core.designsystem.generated.resources.ic_empty_emoji
import bandalart.core.designsystem.generated.resources.ic_option
import bandalart.core.designsystem.generated.resources.ic_share
import bandalart.core.designsystem.generated.resources.option_description
import bandalart.core.designsystem.generated.resources.share_description
import bandalart.core.designsystem.generated.resources.skeleton_complete_ratio
import bandalart.core.designsystem.generated.resources.skeleton_title
import bandalart.core.designsystem.generated.resources.skeleton_trans_animate_label
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.designsystem.theme.Gray100
import com.nexters.bandalart.core.designsystem.theme.Gray200
import com.nexters.bandalart.core.designsystem.theme.Gray300
import com.nexters.bandalart.core.designsystem.theme.Gray400
import com.nexters.bandalart.core.designsystem.theme.Gray50
import com.nexters.bandalart.core.designsystem.theme.Gray600
import com.nexters.bandalart.core.designsystem.theme.Gray900
import com.nexters.bandalart.core.designsystem.theme.neurimboGothicRegularFontFamily
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.feature.home.ui.CompletionRatioProgressBar
import com.nexters.bandalart.feature.home.model.CellType
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.jetbrains.compose.ui.tooling.preview.Preview

data class SkeletonSubCell(
    val rowCnt: Int,
    val colCnt: Int,
    val subCellRowIndex: Int,
    val subCellColIndex: Int,
    val subCellData: BandalartCellEntity? = null,
    val taskCells: List<BandalartCellEntity> = emptyList(),
)

@Composable
fun BandalartSkeletonScreen(
    taskBrush: Brush,
    subBrush: Brush,
    mainBrush: Brush,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Gray50,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 32.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .background(White),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(Res.string.bandalart),
                            color = Gray900,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.W400,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(start = 20.dp, top = 2.dp),
                            fontFamily = neurimboGothicRegularFontFamily(),
                            lineHeight = 20.sp,
                            letterSpacing = (-0.56).sp,
                        )
                    }
                }
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Gray100,
                )
                Column(modifier.padding(horizontal = 16.dp)) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.wrapContentHeight()) {
                        Column {
                            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(52.dp)
                                            .aspectRatio(1f)
                                            .background(Gray100),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = vectorResource(Res.drawable.ic_empty_emoji),
                                            contentDescription = stringResource(Res.string.empty_emoji_description),
                                            tint = Color.Unspecified,
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = vectorResource(Res.drawable.ic_edit),
                                    contentDescription = stringResource(Res.string.edit_description),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 4.dp, y = 4.dp),
                                    tint = Color.Unspecified,
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                            ) {
                                Text(
                                    text = stringResource(Res.string.skeleton_title),
                                    color = Gray900,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.W700,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .background(subBrush),
                                    letterSpacing = (-0.4).sp,
                                )
                                Icon(
                                    imageVector = vectorResource(Res.drawable.ic_option),
                                    contentDescription = stringResource(Res.string.option_description),
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                    tint = Color.Unspecified,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(Res.string.skeleton_complete_ratio),
                            color = Gray600,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.W500,
                            letterSpacing = (-0.24).sp,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    CompletionRatioProgressBar(
                        completionRatio = 0,
                        progressColor = Gray300,
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                BandalartSkeletonChart(
                    taskBrush = taskBrush,
                    subBrush = subBrush,
                    mainBrush = mainBrush,
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Gray100)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 20.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_share),
                            contentDescription = stringResource(Res.string.share_description),
                            tint = Color.Unspecified,
                        )
                        Text(
                            text = stringResource(Res.string.home_share),
                            color = Gray900,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.W700,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BandalartSkeletonChart(
    taskBrush: Brush,
    subBrush: Brush,
    mainBrush: Brush,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints {
        val paddedMaxWidth = remember(maxWidth) { maxWidth - (15.dp * 2) }

        val subCellList = persistentListOf(
            SkeletonSubCell(2, 3, 1, 1, null),
            SkeletonSubCell(3, 2, 1, 0, null),
            SkeletonSubCell(3, 2, 1, 1, null),
            SkeletonSubCell(2, 3, 0, 1, null),
        )

        Layout(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            content = {
                for (index in subCellList.indices) {
                    Box(
                        modifier
                            .layoutId(stringResource(Res.string.home_layout_id, index + 1))
                            .clip(RoundedCornerShape(12.dp))
                            .background(color = Gray200),
                    ) {
                        SkeletonCellGrid(
                            rows = subCellList[index].rowCnt,
                            cols = subCellList[index].colCnt,
                            subCell = subCellList[index],
                            taskBrush = taskBrush,
                            subBrush = subBrush,
                            mainBrush = mainBrush,
                        )
                    }
                }
                Box(
                    modifier
                        .layoutId(stringResource(Res.string.home_main_id))
                        .clip(RoundedCornerShape(10.dp))
                        .background(brush = taskBrush),
                ) {
                    SkeletonCell(
                        cellType = CellType.MAIN,
                        taskBrush = taskBrush,
                        subBrush = subBrush,
                        mainBrush = mainBrush,
                    )
                }
            },
        ) { measurables, constraints ->
            val sub1 = measurables.first { it.layoutId == "Sub 1" }
            val sub2 = measurables.first { it.layoutId == "Sub 2" }
            val sub3 = measurables.first { it.layoutId == "Sub 3" }
            val sub4 = measurables.first { it.layoutId == "Sub 4" }
            val main = measurables.first { it.layoutId == "Main" }

            val chartWidth = paddedMaxWidth.roundToPx()
            val mainWidth = chartWidth / 5
            val padding = 1.dp.roundToPx()

            val mainConstraints = Constraints.fixed(width = mainWidth, height = mainWidth)
            val sub1Constraints = Constraints.fixed(width = mainWidth * 3 - padding, height = mainWidth * 2 - padding)
            val sub2Constraints = Constraints.fixed(width = mainWidth * 2 - padding, height = mainWidth * 3 - padding)
            val sub3Constraints = Constraints.fixed(width = mainWidth * 2 - padding, height = mainWidth * 3 - padding)
            val sub4Constraints = Constraints.fixed(width = mainWidth * 3 - padding, height = mainWidth * 2 - padding)

            val mainPlaceable = main.measure(mainConstraints)
            val sub1Placeable = sub1.measure(sub1Constraints)
            val sub2Placeable = sub2.measure(sub2Constraints)
            val sub3Placeable = sub3.measure(sub3Constraints)
            val sub4Placeable = sub4.measure(sub4Constraints)

            layout(width = chartWidth, height = chartWidth) {
                mainPlaceable.place(x = mainWidth * 2, y = mainWidth * 2)
                sub1Placeable.place(x = 0, y = 0)
                sub2Placeable.place(x = mainWidth * 3 + padding, y = 0)
                sub3Placeable.place(x = 0, y = mainWidth * 2 + padding)
                sub4Placeable.place(x = mainWidth * 2 + padding, y = mainWidth * 3 + padding)
            }
        }
    }
}

@Composable
fun SkeletonCellGrid(
    rows: Int,
    cols: Int,
    subCell: SkeletonSubCell,
    taskBrush: Brush,
    subBrush: Brush,
    mainBrush: Brush,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        repeat(rows) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                repeat(cols) { colIndex ->
                    val isSubCell = rowIndex == subCell.subCellRowIndex && colIndex == subCell.subCellColIndex
                    SkeletonCell(
                        cellType = if (isSubCell) CellType.SUB else CellType.TASK,
                        skeletonCellInfo = SkeletonCellInfo(
                            isSubCell = isSubCell,
                            colIndex = colIndex,
                            rowIndex = rowIndex,
                            colCnt = cols,
                            rowCnt = rows,
                        ),
                        modifier = Modifier.weight(1f),
                        taskBrush = taskBrush,
                        subBrush = subBrush,
                        mainBrush = mainBrush,
                    )
                }
            }
        }
    }
}

data class SkeletonCellInfo(
    val isSubCell: Boolean = false,
    val colIndex: Int = 2,
    val rowIndex: Int = 2,
    val colCnt: Int = 1,
    val rowCnt: Int = 1,
)

@Composable
fun SkeletonCell(
    cellType: CellType,
    taskBrush: Brush,
    subBrush: Brush,
    mainBrush: Brush,
    modifier: Modifier = Modifier,
    skeletonCellInfo: SkeletonCellInfo = SkeletonCellInfo(),
    outerPadding: Dp = 3.dp,
    innerPadding: Dp = 2.dp,
    mainCellPadding: Dp = 1.dp,
) {
    Box(
        modifier = modifier
            .padding(
                start = if (cellType == CellType.MAIN) mainCellPadding
                else if (skeletonCellInfo.colIndex == 0) outerPadding
                else innerPadding,
                end = if (cellType == CellType.MAIN) mainCellPadding
                else if (skeletonCellInfo.colIndex == skeletonCellInfo.colCnt - 1) outerPadding
                else innerPadding,
                top = if (cellType == CellType.MAIN) mainCellPadding
                else if (skeletonCellInfo.rowIndex == 0) outerPadding
                else innerPadding,
                bottom = if (cellType == CellType.MAIN) mainCellPadding
                else if (skeletonCellInfo.rowIndex == skeletonCellInfo.rowCnt - 1) outerPadding
                else innerPadding,
            )
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (cellType == CellType.MAIN) mainBrush else if (skeletonCellInfo.isSubCell) subBrush else taskBrush),
        contentAlignment = Alignment.Center,
    ) { }
}

// @DevicePreview
@Preview
@Composable
private fun BandalartSkeletonScreenPreview() {
    val shimmerMainColors = listOf(
        Gray200,
        Gray300,
        Gray400,
    )
    val shimmerSubColors = listOf(
        Gray100,
        Gray200,
        Gray300,
    )
    val shimmerTaskColors = listOf(
        White,
        Gray50,
    )

    val transition = rememberInfiniteTransition(label = "Skeleton transition")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                easing = FastOutLinearInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = stringResource(Res.string.skeleton_trans_animate_label),
    )

    val mainBrush = Brush.linearGradient(
        colors = shimmerMainColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value),
    )
    val subBrush = Brush.linearGradient(
        colors = shimmerSubColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value),
    )
    val taskBrush = Brush.linearGradient(
        colors = shimmerTaskColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value),
    )

    BandalartTheme {
        BandalartSkeletonScreen(
            taskBrush = mainBrush,
            subBrush = subBrush,
            mainBrush = taskBrush,
        )
    }
}
