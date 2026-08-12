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

package com.nexters.bandalart.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.nexters.bandalart.BandalartApplication
import com.nexters.bandalart.R
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.di.metro.getAndroidWidgetSubGoals
import com.nexters.bandalart.di.metro.observeAndroidWidgetBandalarts
import com.nexters.bandalart.di.metro.setAndroidWidgetRecentBandalartId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BandalartWidgetConfigurationActivity : ComponentActivity() {
    private val appGraph by lazy { (application as BandalartApplication).appGraph }
    private var state by mutableStateOf<ConfigurationState>(ConfigurationState.Loading)
    private var subGoalLoadJob: Job? = null
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId =
            intent?.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(Activity.RESULT_CANCELED, resultIntent(appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            BandalartTheme(darkTheme = isSystemInDarkTheme()) {
                ConfigurationScreen(
                    state = state,
                    onBandalartSelected = ::selectBandalart,
                    onSubGoalSelected = ::selectSubGoal,
                    onSave = ::saveConfiguration,
                )
            }
        }
        loadConfiguration()
    }

    private fun loadConfiguration() {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@BandalartWidgetConfigurationActivity).getGlanceIdBy(appWidgetId)
            val preferences =
                getAppWidgetState(
                    this@BandalartWidgetConfigurationActivity,
                    PreferencesGlanceStateDefinition,
                    glanceId,
                )
            val selection = preferences.toWidgetSelection()
            val bandalarts = observeAndroidWidgetBandalarts(appGraph).first()
            val selectedBandalartId = selection?.bandalartId?.takeIf { id -> bandalarts.any { it.id == id } }
            val subGoals = selectedBandalartId?.let { loadSubGoals(it) }.orEmpty()
            state =
                ConfigurationState.Ready(
                    bandalarts = bandalarts,
                    subGoals = subGoals,
                    selectedBandalartId = selectedBandalartId,
                    selectedSubGoalId = selection?.subGoalId?.takeIf { id -> subGoals.any { it.id == id } },
                )
        }
    }

    private fun selectBandalart(bandalartId: Long) {
        val current = state as? ConfigurationState.Ready ?: return
        if (current.selectedBandalartId == bandalartId) return
        subGoalLoadJob?.cancel()
        state =
            current.copy(
                subGoals = emptyList(),
                selectedBandalartId = bandalartId,
                selectedSubGoalId =
                    subGoalIdAfterBandalartSelection(
                        currentBandalartId = current.selectedBandalartId,
                        currentSubGoalId = current.selectedSubGoalId,
                        selectedBandalartId = bandalartId,
                    ),
                isLoadingSubGoals = true,
            )
        subGoalLoadJob =
            lifecycleScope.launch {
                val subGoals = loadSubGoals(bandalartId)
                val latest = state as? ConfigurationState.Ready ?: return@launch
                if (latest.selectedBandalartId == bandalartId) {
                    state = latest.copy(subGoals = subGoals, isLoadingSubGoals = false)
                }
            }
    }

    private fun selectSubGoal(subGoalId: Long?) {
        val current = state as? ConfigurationState.Ready ?: return
        state = current.copy(selectedSubGoalId = subGoalId)
    }

    private suspend fun loadSubGoals(bandalartId: Long): List<BandalartCellEntity> =
        getAndroidWidgetSubGoals(appGraph, bandalartId).filter {
            !it.title
                .isNullOrBlank()
        }

    private fun saveConfiguration() {
        val current = state as? ConfigurationState.Ready ?: return
        val selectedBandalartId = current.selectedBandalartId ?: return
        state = current.copy(isSaving = true)
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@BandalartWidgetConfigurationActivity).getGlanceIdBy(appWidgetId)
            saveWidgetConfiguration(
                selection = BandalartWidgetSelection(selectedBandalartId, current.selectedSubGoalId),
                setRecentBandalartId = { bandalartId -> setAndroidWidgetRecentBandalartId(appGraph, bandalartId) },
                persistSelection = { selection ->
                    updateAppWidgetState(this@BandalartWidgetConfigurationActivity, glanceId) { preferences ->
                        preferences.setWidgetSelection(selection.bandalartId, selection.subGoalId)
                    }
                },
            )
            BandalartGlanceWidget().update(this@BandalartWidgetConfigurationActivity, glanceId)
            setResult(Activity.RESULT_OK, resultIntent(appWidgetId))
            finish()
        }
    }
}

private sealed interface ConfigurationState {
    data object Loading : ConfigurationState

    data class Ready(
        val bandalarts: List<BandalartEntity>,
        val subGoals: List<BandalartCellEntity>,
        val selectedBandalartId: Long?,
        val selectedSubGoalId: Long?,
        val isLoadingSubGoals: Boolean = false,
        val isSaving: Boolean = false,
    ) : ConfigurationState
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ConfigurationScreen(
    state: ConfigurationState,
    onBandalartSelected: (Long) -> Unit,
    onSubGoalSelected: (Long?) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.bandalart_widget_config_title)) }) },
    ) { innerPadding ->
        when (state) {
            ConfigurationState.Loading ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.bandalart_widget_loading))
                }

            is ConfigurationState.Ready ->
                ConfigurationContent(
                    state = state,
                    onBandalartSelected = onBandalartSelected,
                    onSubGoalSelected = onSubGoalSelected,
                    onSave = onSave,
                    modifier = Modifier.padding(innerPadding),
                )
        }
    }
}

@Composable
private fun ConfigurationContent(
    state: ConfigurationState.Ready,
    onBandalartSelected: (Long) -> Unit,
    onSubGoalSelected: (Long?) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unnamedGoal = stringResource(R.string.bandalart_widget_unnamed_goal)
    val unnamedSubGoal = stringResource(R.string.bandalart_widget_config_unnamed_subgoal)
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text(
            text = stringResource(R.string.bandalart_widget_config_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.bandalart_widget_config_choose_goal),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (state.bandalarts.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.bandalart_widget_config_create_first),
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            } else {
                items(state.bandalarts, key = { bandalart -> bandalartConfigurationItemKey(bandalart.id) }) { bandalart ->
                    SelectionRow(
                        title = bandalart.title?.ifBlank { null } ?: unnamedGoal,
                        selected = state.selectedBandalartId == bandalart.id,
                        onClick = { onBandalartSelected(bandalart.id) },
                    )
                }
            }
            if (state.selectedBandalartId != null) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        text = stringResource(R.string.bandalart_widget_config_subgoal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.bandalart_widget_config_subgoal_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SelectionRow(
                        title = stringResource(R.string.bandalart_widget_config_none),
                        selected = state.selectedSubGoalId == null,
                        onClick = { onSubGoalSelected(null) },
                    )
                }
                if (state.isLoadingSubGoals) {
                    item { CircularProgressIndicator(modifier = Modifier.padding(16.dp)) }
                } else {
                    items(state.subGoals, key = { subGoal -> subGoalConfigurationItemKey(subGoal.id) }) { subGoal ->
                        SelectionRow(
                            title = subGoal.title?.ifBlank { null } ?: unnamedSubGoal,
                            selected = state.selectedSubGoalId == subGoal.id,
                            onClick = { onSubGoalSelected(subGoal.id) },
                        )
                    }
                }
            }
        }
        Button(
            onClick = onSave,
            enabled = state.selectedBandalartId != null && !state.isSaving,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        ) {
            Text(
                stringResource(
                    if (state.isSaving) {
                        R.string.bandalart_widget_config_saving
                    } else {
                        R.string.bandalart_widget_config_save
                    },
                ),
            )
        }
    }
}

@Composable
private fun SelectionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = title, modifier = Modifier.padding(start = 4.dp))
    }
}

private fun resultIntent(appWidgetId: Int): Intent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

internal fun bandalartConfigurationItemKey(bandalartId: Long): String = "bandalart:$bandalartId"

internal fun subGoalConfigurationItemKey(subGoalId: Long): String = "subGoal:$subGoalId"
