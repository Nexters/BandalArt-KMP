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

package com.nexters.bandalart.feature.home.ui.bandalart

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.add_description
import bandalart.core.designsystem.generated.resources.ic_add_circle_outlined
import bandalart.core.designsystem.generated.resources.rewarded_create_dialog_cancel
import bandalart.core.designsystem.generated.resources.rewarded_create_dialog_confirm
import bandalart.core.designsystem.generated.resources.rewarded_create_dialog_message
import bandalart.core.designsystem.generated.resources.rewarded_create_dialog_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun RewardedBandalartAlertDialog(
    onConfirmClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BandalartActionAlertDialog(
        icon = Res.drawable.ic_add_circle_outlined,
        iconContentDescription = stringResource(Res.string.add_description),
        title = stringResource(Res.string.rewarded_create_dialog_title),
        message = stringResource(Res.string.rewarded_create_dialog_message),
        confirmLabel = stringResource(Res.string.rewarded_create_dialog_confirm),
        cancelLabel = stringResource(Res.string.rewarded_create_dialog_cancel),
        onConfirmClick = onConfirmClick,
        onCancelClick = onCancelClick,
        modifier = modifier,
    )
}
