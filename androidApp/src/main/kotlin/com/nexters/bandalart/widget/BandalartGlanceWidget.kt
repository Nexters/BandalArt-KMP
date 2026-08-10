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

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.nexters.bandalart.BandalartApplication
import com.nexters.bandalart.MainActivity
import com.nexters.bandalart.R
import com.nexters.bandalart.di.metro.getAndroidWidgetSnapshot

class BandalartGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode =
        SizeMode.Responsive(
            setOf(
                SmallWidgetSize,
                MediumWidgetSize,
                LargeWidgetSize,
            ),
        )

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val preferences = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val selection = preferences.toWidgetSelection()
        val appGraph = (context.applicationContext as BandalartApplication).appGraph
        val appWidgetId = (id as? AppWidgetId)?.appWidgetId ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val snapshot =
            selection?.let {
                getAndroidWidgetSnapshot(
                    appGraph = appGraph,
                    bandalartId = it.bandalartId,
                    subGoalId = it.subGoalId,
                )
            }
        val viewState = toWidgetViewState(selection, snapshot)

        provideContent {
            BandalartWidgetContent(
                context = context,
                appWidgetId = appWidgetId,
                state = viewState,
            )
        }
    }
}

class BandalartGlanceWidgetReceiver : androidx.glance.appwidget.GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BandalartGlanceWidget()
}

@OptIn(ExperimentalGlanceApi::class)
@Composable
private fun BandalartWidgetContent(
    context: Context,
    appWidgetId: Int,
    state: BandalartWidgetViewState,
) {
    val size = LocalSize.current
    val layout = resolveWidgetLayout(size.width.value.toInt(), size.height.value.toInt())
    val containerModifier =
        GlanceModifier
            .fillMaxSize()
            .background(R.color.widget_background)
            .appWidgetBackground()
            .cornerRadius(20.dp)
            .padding(widgetPadding(layout))

    when (state) {
        BandalartWidgetViewState.Unconfigured,
        BandalartWidgetViewState.Deleted,
        ->
            Column(
                modifier = containerModifier.clickable(reconfigureWidgetAction(context, appWidgetId)),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                val message =
                    if (state == BandalartWidgetViewState.Unconfigured) {
                        "표시할 반다라트를 선택해 주세요"
                    } else {
                        "선택한 목표를 찾을 수 없어요\n눌러서 다시 설정해 주세요"
                    }
                StatusContent(message = message, compact = layout != BandalartWidgetLayout.LARGE)
            }

        is BandalartWidgetViewState.Content ->
            Column(
                modifier = containerModifier,
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                when (layout) {
                    BandalartWidgetLayout.SMALL -> SmallContent(context = context, state = state)
                    BandalartWidgetLayout.MEDIUM,
                    BandalartWidgetLayout.LARGE,
                    -> DetailContent(context = context, state = state, layout = layout)
                }
            }
    }
}

@Composable
private fun StatusContent(
    message: String,
    compact: Boolean,
) {
    Text(
        text = "BANDALART",
        style = LabelTextStyle,
    )
    Spacer(modifier = GlanceModifier.height(if (compact) 4.dp else 8.dp))
    Text(
        text = message,
        style = BodyTextStyle,
        maxLines = 2,
    )
}

@Composable
private fun SmallContent(
    context: Context,
    state: BandalartWidgetViewState.Content,
) {
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .clickable(openBandalartAction(context, state.bandalartId)),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = state.profileEmoji,
            style = TextStyle(fontSize = 22.sp),
            maxLines = 1,
        )
        Spacer(modifier = GlanceModifier.height(3.dp))
        Text(
            text = state.title,
            style = CompactTitleTextStyle,
            maxLines = 1,
        )
        Spacer(modifier = GlanceModifier.height(3.dp))
        Text(
            text = "${state.completionRatio}%",
            style = CompactRatioTextStyle,
            maxLines = 1,
        )
    }
}

@Composable
private fun DetailContent(
    context: Context,
    state: BandalartWidgetViewState.Content,
    layout: BandalartWidgetLayout,
) {
    val compact = layout == BandalartWidgetLayout.MEDIUM
    Row(
        modifier =
            GlanceModifier
                .fillMaxWidth()
                .clickable(openBandalartAction(context, state.bandalartId)),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = state.profileEmoji,
            style = TextStyle(fontSize = if (compact) 16.sp else 20.sp),
            maxLines = 1,
        )
        Spacer(modifier = GlanceModifier.width(if (compact) 5.dp else 8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = state.title,
                style = if (compact) CompactTitleTextStyle else TitleTextStyle,
                maxLines = 1,
            )
            Text(
                text = "달성률 ${state.completionRatio}%",
                style = CaptionTextStyle,
                maxLines = 1,
            )
        }
    }
    Spacer(modifier = GlanceModifier.height(if (compact) 4.dp else 10.dp))
    Text(
        text = state.subGoalTitle ?: "세부 목표를 선택하지 않았어요",
        style = if (compact) CompactSubGoalTextStyle else SubGoalTextStyle,
        maxLines = 1,
    )
    Spacer(modifier = GlanceModifier.height(if (compact) 2.dp else 6.dp))
    val tasks = state.tasksFor(layout)
    if (tasks.isEmpty()) {
        Text(
            text = "등록된 할 일이 없어요",
            style = CaptionTextStyle,
            maxLines = 1,
        )
    } else {
        tasks.forEach { task ->
            val taskActionModifier =
                state.subGoalId?.let { subGoalId ->
                    GlanceModifier.clickable(
                        actionRunCallback<BandalartWidgetTaskActionCallback>(
                            taskActionParameters(
                                BandalartWidgetTaskActionRequest(
                                    bandalartId = state.bandalartId,
                                    subGoalId = subGoalId,
                                    taskId = task.id,
                                    completed = !task.isCompleted,
                                ),
                            ),
                        ),
                    )
                } ?: GlanceModifier
            Row(
                modifier =
                    GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = if (compact) 0.dp else 2.dp)
                        .then(taskActionModifier),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = if (task.isCompleted) "✓" else "○",
                    style = if (task.isCompleted) CompletedMarkTextStyle else MarkTextStyle,
                    maxLines = 1,
                )
                Spacer(modifier = GlanceModifier.width(if (compact) 4.dp else 6.dp))
                Text(
                    text = task.title,
                    modifier = GlanceModifier.defaultWeight(),
                    style =
                        if (task.isCompleted) {
                            if (compact) CompactCompletedTaskTextStyle else CompletedTaskTextStyle
                        } else {
                            if (compact) CompactBodyTextStyle else BodyTextStyle
                        },
                    maxLines = 1,
                )
            }
        }
    }
}

private fun widgetPadding(layout: BandalartWidgetLayout) =
    when (layout) {
        BandalartWidgetLayout.SMALL -> 10.dp
        BandalartWidgetLayout.MEDIUM -> 10.dp
        BandalartWidgetLayout.LARGE -> 16.dp
    }

@OptIn(ExperimentalGlanceApi::class)
private fun openBandalartAction(
    context: Context,
    bandalartId: Long,
) = actionStartActivity(
    Intent(context, MainActivity::class.java).apply {
        action = BandalartWidgetLaunchRequest.ACTION_OPEN_BANDALART
        data = Uri.parse("bandalart://widget/open/$bandalartId")
        putExtra(BandalartWidgetLaunchRequest.EXTRA_BANDALART_ID, bandalartId)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    },
)

@OptIn(ExperimentalGlanceApi::class)
private fun reconfigureWidgetAction(
    context: Context,
    appWidgetId: Int,
) = actionStartActivity(
    Intent(context, BandalartWidgetConfigurationActivity::class.java).apply {
        action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
        data = Uri.parse("bandalart://widget/configure/$appWidgetId")
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    },
)

private val LabelTextStyle =
    TextStyle(
        color = ColorProvider(R.color.widget_text_secondary),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
    )
private val TitleTextStyle =
    TextStyle(
        color = ColorProvider(R.color.widget_text_primary),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
    )
private val CompactTitleTextStyle =
    TextStyle(
        color = ColorProvider(R.color.widget_text_primary),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )
private val SubGoalTextStyle =
    TextStyle(
        color = ColorProvider(R.color.widget_text_primary),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
    )
private val CompactSubGoalTextStyle =
    TextStyle(
        color = ColorProvider(R.color.widget_text_primary),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
    )
private val BodyTextStyle =
    TextStyle(
        color = ColorProvider(R.color.widget_text_primary),
        fontSize = 12.sp,
    )
private val CompactBodyTextStyle =
    TextStyle(
        color = ColorProvider(R.color.widget_text_primary),
        fontSize = 11.sp,
    )
private val CaptionTextStyle =
    TextStyle(
        color = ColorProvider(R.color.widget_text_secondary),
        fontSize = 11.sp,
    )
private val CompactRatioTextStyle =
    TextStyle(
        color = ColorProvider(R.color.widget_accent),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
private val MarkTextStyle =
    TextStyle(
        color = ColorProvider(R.color.widget_accent),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
    )
private val CompletedMarkTextStyle =
    TextStyle(
        color = ColorProvider(R.color.widget_text_secondary),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
    )
private val CompletedTaskTextStyle =
    TextStyle(
        color = ColorProvider(R.color.widget_text_secondary),
        fontSize = 12.sp,
    )
private val CompactCompletedTaskTextStyle =
    TextStyle(
        color = ColorProvider(R.color.widget_text_secondary),
        fontSize = 11.sp,
    )

private val SmallWidgetSize = DpSize(width = 110.dp, height = 110.dp)
private val MediumWidgetSize = DpSize(width = 250.dp, height = 110.dp)
private val LargeWidgetSize = DpSize(width = 250.dp, height = 240.dp)
