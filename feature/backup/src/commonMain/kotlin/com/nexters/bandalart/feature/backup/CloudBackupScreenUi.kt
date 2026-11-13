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

package com.nexters.bandalart.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.backup_back
import bandalart.core.designsystem.generated.resources.backup_created
import bandalart.core.designsystem.generated.resources.action_cancel
import bandalart.core.designsystem.generated.resources.backup_create_confirm
import bandalart.core.designsystem.generated.resources.backup_create_confirm_body
import bandalart.core.designsystem.generated.resources.backup_create_confirm_title
import bandalart.core.designsystem.generated.resources.backup_description
import bandalart.core.designsystem.generated.resources.backup_error
import bandalart.core.designsystem.generated.resources.backup_loading
import bandalart.core.designsystem.generated.resources.backup_not_found
import bandalart.core.designsystem.generated.resources.backup_not_supported
import bandalart.core.designsystem.generated.resources.backup_now
import bandalart.core.designsystem.generated.resources.backup_restore
import bandalart.core.designsystem.generated.resources.backup_restore_confirm
import bandalart.core.designsystem.generated.resources.backup_restore_confirm_body
import bandalart.core.designsystem.generated.resources.backup_restore_confirm_title
import bandalart.core.designsystem.generated.resources.backup_restored
import bandalart.core.designsystem.generated.resources.backup_start_fresh
import bandalart.core.designsystem.generated.resources.backup_status_existing
import bandalart.core.designsystem.generated.resources.backup_status_none
import bandalart.core.designsystem.generated.resources.backup_title
import bandalart.core.designsystem.generated.resources.ic_cloud_download
import bandalart.core.designsystem.generated.resources.ic_history
import bandalart.core.designsystem.generated.resources.rewarded_ad_unavailable
import com.nexters.bandalart.core.common.RewardedAdGateway
import com.nexters.bandalart.core.common.RewardedAdPurpose
import com.nexters.bandalart.core.common.RewardedAdResult
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily
import com.nexters.bandalart.core.navigation.CloudBackupScreen
import com.nexters.bandalart.core.ui.LocalShowSnackbar
import com.nexters.bandalart.feature.home.ui.bandalart.BandalartActionAlertDialog
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import zone.ien.hig.CupertinoLiquidButton
import zone.ien.hig.CupertinoLiquidButtonDefaults
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.adaptive.AdaptiveButton
import zone.ien.hig.adaptive.AdaptiveCircularProgressIndicator
import zone.ien.hig.adaptive.AdaptiveIconButton
import zone.ien.hig.adaptive.AdaptiveTextButton
import zone.ien.hig.adaptive.AdaptiveWidget
import zone.ien.hig.adaptive.ExperimentalAdaptiveApi
import zone.ien.hig.utils.rememberDefaultBackdrop

private const val SNACKBAR_DURATION_MILLIS = 1500L

@CircuitInject(CloudBackupScreen::class, AppScope::class)
@Inject
@OptIn(ExperimentalAdaptiveApi::class, ExperimentalCupertinoApi::class)
@Composable
internal fun CloudBackup(
    state: CloudBackupUiState,
    rewardedAdGateway: RewardedAdGateway,
    modifier: Modifier = Modifier,
) {
    val showSnackbar = LocalShowSnackbar.current
    val latestEventSink by rememberUpdatedState(state.eventSink)
    LaunchedEffect(state.rewardedAdRequestId) {
        val requestId = state.rewardedAdRequestId ?: return@LaunchedEffect
        try {
            val result =
                try {
                    rewardedAdGateway.show(requestId, RewardedAdPurpose.CLOUD_BACKUP)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    RewardedAdResult.FAILED
                }
            latestEventSink(CloudBackupUiState.Event.RewardedAdFinished(requestId, result))
        } finally {
            rewardedAdGateway.consume(requestId)
        }
    }
    LaunchedEffect(state.effect) {
        when (state.effect) {
            CloudBackupUiState.Effect.ShowAdUnavailableSnackbar -> {
                showSnackbarForDuration(getString(Res.string.rewarded_ad_unavailable), showSnackbar)
                state.eventSink(CloudBackupUiState.Event.ConsumeEffect)
            }
            null -> Unit
        }
    }

    if (state.showCreateBackupConfirmation) {
        CreateBackupConfirmationDialog(state.eventSink)
    }
    if (state.showRestoreConfirmation) {
        RestoreConfirmationDialog(state.eventSink)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
        ) {
            BackupHeader(
                showBack = state.entryPoint == CloudBackupScreen.EntryPoint.SETTINGS,
                onBack = { state.eventSink(CloudBackupUiState.Event.Back) },
            )
            Text(
                text = stringResource(Res.string.backup_description),
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = pretendardFontFamily(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text =
                            when {
                                !state.isSupported -> stringResource(Res.string.backup_not_supported)
                                state.metadata == null -> stringResource(Res.string.backup_status_none)
                                else ->
                                    stringResource(
                                        Res.string.backup_status_existing,
                                        state.metadata.bandalartCount,
                                        state.metadata.updatedAt.toDisplayDateTime(),
                                    )
                            },
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = pretendardFontFamily(),
                        fontWeight = FontWeight.SemiBold,
                        autoSize =
                            TextAutoSize.StepBased(
                                minFontSize = 12.sp,
                                maxFontSize = 16.sp,
                                stepSize = 0.5.sp,
                            ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (state.isLoading) {
                        Row(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AdaptiveCircularProgressIndicator()
                            Text(
                                text = stringResource(Res.string.backup_loading),
                                fontFamily = pretendardFontFamily(),
                            )
                        }
                    }
                    state.result?.let { result ->
                        Text(
                            text = stringResource(result.messageResource()),
                            fontFamily = pretendardFontFamily(),
                            color =
                                if (result == CloudBackupUiState.Result.ERROR) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (state.entryPoint == CloudBackupScreen.EntryPoint.SETTINGS) {
                AdaptiveButton(
                    onClick = { state.eventSink(CloudBackupUiState.Event.CreateBackup) },
                    enabled = state.isSupported && !state.isLoading && state.rewardedAdRequestId == null,
                    modifier = Modifier.fillMaxWidth(),
                    adaptation = {
                        cupertino {
                            val primary = MaterialTheme.colorScheme.primary
                            colors =
                                CupertinoLiquidButtonDefaults.glassProminentButtonColors(
                                    lightTintColor = primary,
                                    darkTintColor = primary,
                                )
                        }
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.backup_now),
                        fontFamily = pretendardFontFamily(),
                    )
                }
            }
            val isRestoreEnabled =
                state.isSupported &&
                    !state.isLoading &&
                    state.rewardedAdRequestId == null &&
                    state.metadata != null
            val restoreLabel: @Composable RowScope.() -> Unit = {
                Text(
                    text = stringResource(Res.string.backup_restore),
                    fontFamily = pretendardFontFamily(),
                )
            }
            // adaptive outlined 버튼이 없어 Android는 기존 OutlinedButton, iOS는 유리 버튼으로 그린다.
            AdaptiveWidget(
                material = {
                    OutlinedButton(
                        onClick = { state.eventSink(CloudBackupUiState.Event.RestoreBackup) },
                        enabled = isRestoreEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        content = restoreLabel,
                    )
                },
                cupertino = {
                    CupertinoLiquidButton(
                        onClick = { state.eventSink(CloudBackupUiState.Event.RestoreBackup) },
                        backdrop = rememberDefaultBackdrop(),
                        enabled = isRestoreEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        content = restoreLabel,
                    )
                },
            )
            if (state.entryPoint == CloudBackupScreen.EntryPoint.STARTUP) {
                AdaptiveTextButton(
                    onClick = { state.eventSink(CloudBackupUiState.Event.StartFresh) },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    adaptation = {
                        // AdaptiveTextButton의 Material 기본값은 Button 기준이라 TextButton 기본값으로 되돌린다.
                        material {
                            elevation = null
                            shape = ButtonDefaults.textShape
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        }
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.backup_start_fresh),
                        fontFamily = pretendardFontFamily(),
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalAdaptiveApi::class)
@Composable
private fun BackupHeader(
    showBack: Boolean,
    onBack: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            AdaptiveIconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.backup_back),
                )
            }
            // iOS 유리 버튼은 배경이 있어 제목과 붙어 보이므로 간격을 둔다. Android는 기존 간격을 유지한다.
            AdaptiveWidget(
                material = {},
                cupertino = { Spacer(modifier = Modifier.width(12.dp)) },
            )
        }
        Text(
            text = stringResource(Res.string.backup_title),
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = pretendardFontFamily(),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RestoreConfirmationDialog(eventSink: (CloudBackupUiState.Event) -> Unit) {
    BandalartActionAlertDialog(
        icon = Res.drawable.ic_history,
        iconContentDescription = stringResource(Res.string.backup_restore),
        title = stringResource(Res.string.backup_restore_confirm_title),
        message = stringResource(Res.string.backup_restore_confirm_body),
        confirmLabel = stringResource(Res.string.backup_restore_confirm),
        cancelLabel = stringResource(Res.string.action_cancel),
        onConfirmClick = { eventSink(CloudBackupUiState.Event.ConfirmRestore) },
        onCancelClick = { eventSink(CloudBackupUiState.Event.DismissRestoreConfirmation) },
    )
}

@Composable
private fun CreateBackupConfirmationDialog(eventSink: (CloudBackupUiState.Event) -> Unit) {
    BandalartActionAlertDialog(
        icon = Res.drawable.ic_cloud_download,
        iconContentDescription = stringResource(Res.string.backup_now),
        title = stringResource(Res.string.backup_create_confirm_title),
        message = stringResource(Res.string.backup_create_confirm_body),
        confirmLabel = stringResource(Res.string.backup_create_confirm),
        cancelLabel = stringResource(Res.string.action_cancel),
        onConfirmClick = { eventSink(CloudBackupUiState.Event.ConfirmCreateBackup) },
        onCancelClick = { eventSink(CloudBackupUiState.Event.DismissCreateBackupConfirmation) },
    )
}

private suspend fun showSnackbarForDuration(
    message: String,
    showSnackbar: suspend (String) -> Boolean,
) {
    coroutineScope {
        val snackbarJob = launch { showSnackbar(message) }
        delay(SNACKBAR_DURATION_MILLIS)
        snackbarJob.cancel()
    }
}

private fun String.toDisplayDateTime(): String =
    runCatching {
        val localDateTime = Instant.parse(this).toLocalDateTime(TimeZone.currentSystemDefault())
        buildString {
            append(localDateTime.year)
            append('-')
            append(localDateTime.monthNumber.toTwoDigits())
            append('-')
            append(localDateTime.dayOfMonth.toTwoDigits())
            append(' ')
            append(localDateTime.hour.toTwoDigits())
            append(':')
            append(localDateTime.minute.toTwoDigits())
        }
    }.getOrElse {
        take(16).replace('T', ' ').ifBlank { this }
    }

private fun Int.toTwoDigits(): String = toString().padStart(2, '0')

private fun CloudBackupUiState.Result.messageResource() =
    when (this) {
        CloudBackupUiState.Result.BACKUP_CREATED -> Res.string.backup_created
        CloudBackupUiState.Result.RESTORED -> Res.string.backup_restored
        CloudBackupUiState.Result.BACKUP_NOT_FOUND -> Res.string.backup_not_found
        CloudBackupUiState.Result.ERROR -> Res.string.backup_error
    }
