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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.bottomsheet_done
import bandalart.core.designsystem.generated.resources.bottomsheet_reset
import bandalart.core.designsystem.generated.resources.datepicker_day
import bandalart.core.designsystem.generated.resources.datepicker_month
import bandalart.core.designsystem.generated.resources.datepicker_year
import com.nexters.bandalart.core.common.extension.LocalDateTime
import com.nexters.bandalart.core.common.extension.toLocalDateTime
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.designsystem.theme.Gray200
import com.nexters.bandalart.core.designsystem.theme.Gray900
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private val years = (2000..2050).map { it }
private val monthsNumber = (1..12).map { it }
private val days = (1..31).map { it }

@Composable
fun BandalartDatePicker(
    onDueDateSelect: (LocalDateTime?) -> Unit,
    currentDueDate: LocalDateTime,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val chosenYear = remember { mutableStateOf(currentDueDate.year.toString()) }
        val chosenMonth = remember { mutableStateOf(currentDueDate.monthValue.toString()) }
        val chosenDay = remember { mutableStateOf(currentDueDate.dayOfMonth.toString()) }

        Row(
            modifier = Modifier
                .align(Alignment.End)
                .padding(
                    top = 14.dp,
                    end = 20.dp,
                    bottom = 14.dp,
                ),
        ) {
            Text(
                text = stringResource(Res.string.bottomsheet_reset),
                color = Gray900,
                fontSize = 16.sp,
                fontWeight = FontWeight.W700,
                modifier = Modifier
                    .clickable {
                        onDueDateSelect(LocalDateTime.now())
                    },
                fontFamily = pretendardFontFamily(),
            )
            Text(
                text = stringResource(Res.string.bottomsheet_done),
                color = Gray900,
                fontSize = 16.sp,
                fontWeight = FontWeight.W700,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .clickable {
                        onDueDateSelect(selectedDateWithValidate(chosenYear.value, chosenMonth.value, chosenDay.value))
                    },
                fontFamily = pretendardFontFamily(),
            )
        }
        DateSelectionSection(
            onYearChosen = { chosenYear.value = it },
            onMonthChosen = { chosenMonth.value = it },
            onDayChosen = { chosenDay.value = it },
            currentYear = chosenYear.value.toInt(),
            currentMonth = chosenMonth.value.toInt(),
            currentDay = chosenDay.value.toInt(),
        )
    }
}

@Composable
fun DateSelectionSection(
    onYearChosen: (String) -> Unit,
    onMonthChosen: (String) -> Unit,
    onDayChosen: (String) -> Unit,
    currentYear: Int,
    currentMonth: Int,
    currentDay: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        val yearOffset = Int.MAX_VALUE / 2 - ((Int.MAX_VALUE / 2) % years.size) + years.indexOf(currentYear)
        val monthOffset = Int.MAX_VALUE / 2 - ((Int.MAX_VALUE / 2) % monthsNumber.size) + (currentMonth - 1)
        val dayOffset = Int.MAX_VALUE / 2 - ((Int.MAX_VALUE / 2) % days.size) + (currentDay - 1)

        Box(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Gray200)
                .align(Alignment.Center),
        )
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) {
            InfiniteItemsPicker(
                modifier = Modifier.weight(1f),
                items = years.toImmutableList(),
                isYear = true,
                isMonth = true,
                firstIndex = yearOffset,
                onItemSelected = onYearChosen,
            )
            InfiniteItemsPicker(
                modifier = Modifier.weight(1f),
                items = monthsNumber.toImmutableList(),
                isYear = false,
                isMonth = true,
                firstIndex = monthOffset,
                onItemSelected = onMonthChosen,
            )
            InfiniteItemsPicker(
                modifier = Modifier.weight(1f),
                items = days.toImmutableList(),
                isYear = false,
                isMonth = false,
                firstIndex = dayOffset,
                onItemSelected = onDayChosen,
            )
        }
    }
}

@Composable
fun InfiniteItemsPicker(
    items: ImmutableList<Int>,
    isYear: Boolean,
    isMonth: Boolean,
    firstIndex: Int,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(firstIndex)
    val currentValue = remember { mutableStateOf(items[firstIndex % items.size].toString()) }

    LaunchedEffect(key1 = !listState.isScrollInProgress) {
        if (currentValue.value.isNotEmpty()) {
            onItemSelected(currentValue.value)
            listState.animateScrollToItem(index = listState.firstVisibleItemIndex)
        }
    }

    Box(modifier = modifier.height(180.dp)) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            state = listState,
            contentPadding = PaddingValues(vertical = 70.dp),
            modifier = Modifier.fillMaxHeight(),
        ) {
            items(count = Int.MAX_VALUE) { index ->
                val itemIndex = index % items.size
                val visibleItemsInfo = listState.layoutInfo.visibleItemsInfo
                val middleIndex = visibleItemsInfo.size / 2
                val isMiddleItem = visibleItemsInfo.getOrNull(middleIndex)?.index == index

                if (isMiddleItem) {
                    currentValue.value = items[itemIndex].toString()
                }

                Box(
                    modifier = Modifier.height(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when {
                            isYear -> stringResource(Res.string.datepicker_year, items[itemIndex].toString())
                            isMonth -> stringResource(Res.string.datepicker_month, items[itemIndex].toString())
                            else -> stringResource(Res.string.datepicker_day, items[itemIndex].toString())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (isMiddleItem) 1f else 0.6f),
                        color = Gray900,
                        fontSize = if (isMiddleItem) 20.sp else 17.sp,
                        fontWeight = if (isMiddleItem) FontWeight.W500 else FontWeight.W400,
                        textAlign = TextAlign.Center,
                        fontFamily = pretendardFontFamily(),
                    )
                }
            }
        }
    }
}

fun selectedDateWithValidate(year: String, month: String, day: String): LocalDateTime {
    val yearNum = year.filter { it.isDigit() }.toInt()
    val monthNum = month.filter { it.isDigit() }.toInt()
    val dayNum = day.filter { it.isDigit() }.toInt()

    // 각 월의 최대 일수 계산
    val maxDaysInMonth = when (monthNum) {
        2 -> if (isLeapYear(yearNum)) 29 else 28 // 2월: 윤년 고려
        4, 6, 9, 11 -> 30 // 30일까지 있는 달
        else -> 31 // 그 외 달은 31일
    }

    // 일자 유효성 검사 및 보정
    val validatedDay = dayNum.coerceAtMost(maxDaysInMonth)

    return "$yearNum-${monthNum.toString().padStart(2, '0')}-${validatedDay.toString().padStart(2, '0')}T00:00".toLocalDateTime()
}

// 윤년 계산 함수
private fun isLeapYear(year: Int): Boolean {
    return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
}

// @ComponentPreview
@Preview
@Composable
private fun BandalartDatePickerPreview() {
    BandalartTheme {
        BandalartDatePicker(
            onDueDateSelect = {},
            currentDueDate = LocalDateTime.now(),
        )
    }
}

// @ComponentPreview
@Preview
@Composable
private fun DateSelectionSectionPreview() {
    BandalartTheme {
        DateSelectionSection(
            onYearChosen = {},
            onMonthChosen = {},
            onDayChosen = {},
            currentYear = 2024,
            currentMonth = 12,
            currentDay = 31,
        )
    }
}

// @ComponentPreview
@Preview
@Composable
private fun InfiniteYearItemsPickerPreview() {
    BandalartTheme {
        InfiniteItemsPicker(
            items = years.toImmutableList(),
            isYear = true,
            isMonth = false,
            firstIndex = Int.MAX_VALUE / 2 + (2024 - years.first()),
            onItemSelected = {},
        )
    }
}

// @ComponentPreview
@Preview
@Composable
private fun InfiniteMonthItemsPickerPreview() {
    BandalartTheme {
        InfiniteItemsPicker(
            items = monthsNumber.toImmutableList(),
            isYear = false,
            isMonth = true,
            firstIndex = Int.MAX_VALUE / 2 + (31 - 1),
            onItemSelected = {},
        )
    }
}

// @ComponentPreview
@Preview
@Composable
private fun InfiniteDayItemsPickerPreview() {
    BandalartTheme {
        InfiniteItemsPicker(
            items = days.toImmutableList(),
            isYear = false,
            isMonth = false,
            firstIndex = Int.MAX_VALUE / 2 + (31 - 3),
            onItemSelected = {},
        )
    }
}
