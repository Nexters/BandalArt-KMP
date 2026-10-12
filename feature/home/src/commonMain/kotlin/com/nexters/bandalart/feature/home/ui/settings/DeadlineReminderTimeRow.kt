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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.action_cancel
import bandalart.core.designsystem.generated.resources.bottomsheet_done
import bandalart.core.designsystem.generated.resources.ic_notifications_outlined
import bandalart.core.designsystem.generated.resources.settings_deadline_reminder_time
import bandalart.core.designsystem.generated.resources.settings_deadline_reminder_time_dialog_title
import com.nexters.bandalart.core.common.Language
import com.nexters.bandalart.core.common.getLocale
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily
import com.nexters.bandalart.feature.home.ui.bandalart.BandalartActionAlertDialog
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeadlineReminderTimeRow(
    time: LocalTime,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.settings_deadline_reminder_time),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontFamily = pretendardFontFamily(),
            fontWeight = FontWeight.W600,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = time.toReminderTimeText(getLocale().language),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            fontFamily = pretendardFontFamily(),
        )
    }
}

@Composable
internal fun DeadlineReminderTimePickerDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTime by remember { mutableStateOf(initialTime) }
    BandalartActionAlertDialog(
        icon = Res.drawable.ic_notifications_outlined,
        iconContentDescription = null,
        title = stringResource(Res.string.settings_deadline_reminder_time_dialog_title),
        message = null,
        confirmLabel = stringResource(Res.string.bottomsheet_done),
        cancelLabel = stringResource(Res.string.action_cancel),
        onConfirmClick = { onConfirm(selectedTime) },
        onCancelClick = onDismiss,
    ) {
        DeadlineReminderTimePicker(
            initialTime = initialTime,
            onTimeChange = { selectedTime = it },
        )
    }
}

internal fun LocalTime.toReminderTimeText(language: Language): String {
    val isAm = hour < 12
    val displayHour = (hour % 12).let { if (it == 0) 12 else it }
    val minuteText = minute.toString().padStart(2, '0')
    return when (language) {
        Language.KOREAN -> "${if (isAm) "오전" else "오후"} $displayHour:$minuteText"
        Language.ENGLISH -> "$displayHour:$minuteText ${if (isAm) "AM" else "PM"}"
        Language.JAPANESE -> "${if (isAm) "午前" else "午後"}$displayHour:$minuteText"
    }
}

@Composable
internal expect fun DeadlineReminderTimePicker(
    initialTime: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
)
