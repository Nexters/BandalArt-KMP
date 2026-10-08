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

package com.nexters.bandalart.feature.home.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.datetime.LocalTime
import zone.ien.hig.CupertinoTimePicker
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.rememberCupertinoTimePickerState

@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal actual fun DeadlineReminderTimePicker(
    initialTime: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
) {
    val state =
        rememberCupertinoTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = false,
        )
    val currentOnTimeChange = rememberUpdatedState(onTimeChange)
    LaunchedEffect(state) {
        snapshotFlow { LocalTime(hour = state.hour, minute = state.minute) }
            .collect { currentOnTimeChange.value(it) }
    }
    CupertinoTimePicker(
        state = state,
        containerColor = MaterialTheme.colorScheme.surface,
    )
}
