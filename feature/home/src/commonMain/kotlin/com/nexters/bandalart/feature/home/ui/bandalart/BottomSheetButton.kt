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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.bottomsheet_delete
import bandalart.core.designsystem.generated.resources.bottomsheet_done
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import org.jetbrains.compose.resources.stringResource
import zone.ien.hig.CupertinoLiquidButton
import zone.ien.hig.CupertinoLiquidButtonDefaults
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.adaptive.AdaptiveWidget
import zone.ien.hig.adaptive.ExperimentalAdaptiveApi
import zone.ien.hig.utils.rememberDefaultBackdrop
import androidx.compose.ui.tooling.preview.Preview

/** iOS(Cupertino)는 유리 버튼, 그 외는 기존 Material 버튼으로 그린다. */
@OptIn(ExperimentalAdaptiveApi::class, ExperimentalCupertinoApi::class)
@Composable
fun BottomSheetDeleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(Res.string.bottomsheet_delete)
    AdaptiveWidget(
        material = {
            FilledIconButton(
                onClick = onClick,
                modifier = modifier.height(56.dp),
                colors =
                    IconButtonColors(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.onSurface,
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            ) {
                BottomSheetButtonText(
                    text = text,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        cupertino = {
            CupertinoLiquidButton(
                onClick = onClick,
                backdrop = rememberDefaultBackdrop(),
                modifier = modifier.height(56.dp),
            ) {
                BottomSheetButtonText(
                    text = text,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    )
}

/** iOS(Cupertino)는 primary 색 유리 버튼, 그 외는 기존 Material 버튼으로 그린다. */
@OptIn(ExperimentalAdaptiveApi::class, ExperimentalCupertinoApi::class)
@Composable
fun BottomSheetCompleteButton(
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(Res.string.bottomsheet_done)
    val textColor =
        if (isEnabled) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    AdaptiveWidget(
        material = {
            FilledIconButton(
                onClick = onClick,
                modifier = modifier.height(56.dp),
                colors =
                    IconButtonColors(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.onPrimary,
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                enabled = isEnabled,
            ) {
                BottomSheetButtonText(text = text, color = textColor)
            }
        },
        cupertino = {
            val primary = MaterialTheme.colorScheme.primary
            CupertinoLiquidButton(
                onClick = onClick,
                backdrop = rememberDefaultBackdrop(),
                modifier = modifier.height(56.dp),
                enabled = isEnabled,
                colors =
                    CupertinoLiquidButtonDefaults.glassProminentButtonColors(
                        lightTintColor = primary,
                        darkTintColor = primary,
                    ),
            ) {
                BottomSheetButtonText(text = text, color = textColor)
            }
        },
    )
}

// @ComponentPreview
@Preview
@Composable
private fun BottomSheetDeleteButtonPreview() {
    BandalartTheme {
        BottomSheetDeleteButton(
            onClick = {},
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
        )
    }
}

// @ComponentPreview
@Preview
@Composable
private fun BottomSheetCompleteButtonPreview() {
    BandalartTheme {
        BottomSheetCompleteButton(
            isEnabled = false,
            onClick = {},
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
        )
    }
}
