package com.mahyarmozafar.tik.widget

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.view.View
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mahyarmozafar.tik.MainActivity
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.TikApplication
import com.mahyarmozafar.tik.app.localized
import com.mahyarmozafar.tik.logic.TaskFilter
import com.mahyarmozafar.tik.model.AppLanguage
import com.mahyarmozafar.tik.model.Priority
import com.mahyarmozafar.tik.model.Task
import com.mahyarmozafar.tik.model.TikSettings
import com.mahyarmozafar.tik.ui.theme.SystemColor
import com.mahyarmozafar.tik.ui.theme.system
import java.time.Instant
import java.util.Locale

/** A task turned into plain values, ready to draw in the widget. */
data class WidgetTask(
    val id: String,
    val title: String,
    val isDone: Boolean,
    val time: String?,
    val isLate: Boolean,
    val priority: Priority,
)

data class WidgetState(
    val tasks: List<WidgetTask>,
    val doneCount: Int,
    val totalCount: Int,
    val settings: TikSettings,
    val now: Instant,
) {
    val openCount: Int get() = totalCount - doneCount
    val progress: Float get() = if (totalCount == 0) 0f else doneCount.toFloat() / totalCount
}

/** Today's tasks the way the Today screen shows them. */
fun widgetState(tasks: List<Task>, settings: TikSettings, now: Instant): WidgetState {
    val formatting = settings.formatting()
    val calendar = formatting.calendar
    val today = TaskFilter.today(tasks, now, calendar, settings.sortOrder)
    val shown = if (settings.showCompletedInToday) today else today.filter { !it.isDone }
    return WidgetState(
        tasks = shown.map { task ->
            WidgetTask(
                id = task.id,
                title = task.title,
                isDone = task.isDone,
                time = task.dueDate?.takeIf { task.hasTime }?.let(formatting::time),
                isLate = TaskFilter.isLate(task, now, calendar),
                priority = task.priority,
            )
        },
        doneCount = today.count { it.isDone },
        totalCount = today.size,
        settings = settings,
        now = now,
    )
}

/** The Home Screen widget with today's tasks. Tasks can be ticked right on it. */
class TodayWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(Small, Medium, Large))
    override val previewSizeMode = SizeMode.Responsive(setOf(Small, Medium, Large))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TikApplication
        val firstSettings = app.settings.current()
        val firstTasks = app.store.allTasks()
        provideContent {
            // The widget follows every change while it is on screen.
            val settings by app.settings.settings.collectAsState(firstSettings)
            val tasks by app.store.tasks.collectAsState(firstTasks)
            WidgetContent(widgetState(tasks, settings, Instant.now()))
        }
    }

    /** What the widget list shows before the widget is added. */
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val settings = (context.applicationContext as TikApplication).settings.current()
        val texts = context.localized(settings.language)
        val time = settings.formatting().time(Instant.now().plusSeconds(3600))
        val sample = listOf(
            WidgetTask("1", texts.getString(R.string.sample_reply), false, time, false, Priority.High),
            WidgetTask("2", texts.getString(R.string.sample_call), false, null, false, Priority.Medium),
            WidgetTask("3", texts.getString(R.string.sample_groceries), false, null, false, Priority.None),
            WidgetTask("4", texts.getString(R.string.sample_run), true, null, false, Priority.None),
        )
        provideContent {
            WidgetContent(WidgetState(sample, doneCount = 1, totalCount = 4, settings = settings, now = Instant.now()))
        }
    }

    companion object {
        private val Small = DpSize(110.dp, 110.dp)
        private val Medium = DpSize(250.dp, 110.dp)
        private val Large = DpSize(250.dp, 250.dp)

        suspend fun updateAll(context: Context) {
            runCatching { TodayWidget().updateAll(context) }
        }

        /** Android 15 and newer show a live preview in the widget list. */
        suspend fun publishPreviews(context: Context) {
            if (Build.VERSION.SDK_INT < 35) return
            runCatching { GlanceAppWidgetManager(context).setWidgetPreviews(TodayWidgetReceiver::class) }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

/** Ticks or unticks a task from the widget, without opening the app. */
class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TaskId] ?: return
        (context.applicationContext as TikApplication).model.toggle(id)
    }

    companion object {
        val TaskId = ActionParameters.Key<String>("taskId")
    }
}

/**
 * Colors, and which way things run. The launcher lays the widget out in the phone's language,
 * so when Tik's language runs the other way, the rows are flipped by hand.
 */
private class WidgetLook(settings: TikSettings, context: Context) {
    val rtl = settings.language == AppLanguage.Farsi
    val flip = rtl != (Resources.getSystem().configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL)
    val startAlign = if (rtl) TextAlign.Right else TextAlign.Left
    val startColumn = if (flip) Alignment.End else Alignment.Start
    val english = settings.language == AppLanguage.English

    val accent: ColorProvider = if (settings.wallpaperColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ColorProvider(dynamicLightColorScheme(context).primary, dynamicDarkColorScheme(context).primary)
    } else {
        ColorProvider(settings.accent.system.color(false), settings.accent.system.color(true))
    }
    val primary = ColorProvider(Color.Black, Color.White)
    val secondary = ColorProvider(Color(0xFF8A8A8E), Color(0xFF98989F))
    val late = ColorProvider(SystemColor.Red.color(false), SystemColor.Red.color(true))
    val track = ColorProvider(Color.Black.copy(alpha = 0.08f), Color.White.copy(alpha = 0.14f))
    val white = ColorProvider(Color.White, Color.White)

    fun priority(priority: Priority): ColorProvider? = when (priority) {
        Priority.None -> null
        Priority.Low -> ColorProvider(SystemColor.Blue.color(false), SystemColor.Blue.color(true))
        Priority.Medium -> ColorProvider(SystemColor.Orange.color(false), SystemColor.Orange.color(true))
        Priority.High -> ColorProvider(SystemColor.Red.color(false), SystemColor.Red.color(true))
    }
}

/** A row whose children are flipped when the widget has to run the other way. */
@Composable
private fun FlipRow(look: WidgetLook, modifier: GlanceModifier = GlanceModifier, children: List<@Composable RowScope.() -> Unit>) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        (if (look.flip) children.reversed() else children).forEach { it() }
    }
}

private fun openApp(context: Context, what: String) = actionStartActivity(
    Intent(Intent.ACTION_VIEW, "tik://$what".toUri(), context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
)

@Composable
private fun WidgetContent(state: WidgetState) {
    val context = LocalContext.current
    val size = LocalSize.current
    val look = remember(state.settings) { WidgetLook(state.settings, context) }
    val texts = remember(state.settings.language) { context.localized(state.settings.language) }
    val small = size.width < 200.dp
    val large = size.height >= 200.dp

    Box(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background(ImageProvider(R.drawable.widget_background))
            .clickable(openApp(context, "today")),
    ) {
        Box(GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_glow), colorFilter = ColorFilter.tint(look.accent))) {}
        Column(GlanceModifier.fillMaxSize().padding(14.dp), horizontalAlignment = look.startColumn) {
            Header(state, look, texts, style = if (small) HeaderStyle.Compact else if (large) HeaderStyle.Full else HeaderStyle.Inline)
            Spacer(GlanceModifier.height(if (large) 10.dp else 8.dp))
            if (large && state.totalCount > 0) {
                LinearProgressIndicator(
                    progress = state.progress,
                    modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                    color = look.accent,
                    backgroundColor = look.track,
                )
                Spacer(GlanceModifier.height(10.dp))
            }
            val shown = if (small) state.tasks.filter { !it.isDone } else state.tasks
            if (shown.isEmpty()) {
                EmptyState(state, look, texts)
            } else {
                val limit = if (large) 8 else 3
                shown.take(limit).forEachIndexed { index, task ->
                    if (index > 0) Spacer(GlanceModifier.height(if (large) 10.dp else 8.dp))
                    TaskLine(task, look, compact = small)
                }
                if (large && shown.size > limit) {
                    Spacer(GlanceModifier.height(8.dp))
                    Text(
                        texts.getString(R.string.n_more, (shown.size - limit).toString()),
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = look.secondary, textAlign = look.startAlign),
                        modifier = GlanceModifier.fillMaxWidth(),
                    )
                }
                if (small && state.totalCount > 0) {
                    Spacer(GlanceModifier.defaultWeight())
                    LinearProgressIndicator(
                        progress = state.progress,
                        modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                        color = look.accent,
                        backgroundColor = look.track,
                    )
                }
            }
        }
    }
}

private enum class HeaderStyle { Full, Inline, Compact }

/** The date, "Today", how many are done, and a + button. */
@Composable
private fun Header(state: WidgetState, look: WidgetLook, texts: Context, style: HeaderStyle) {
    val context = LocalContext.current
    val formatting = state.settings.formatting()
    val dateText = if (style == HeaderStyle.Full) formatting.fullDay(state.now) else formatting.shortDay(state.now)
    val date = if (look.english) dateText.uppercase(Locale.ENGLISH) else dateText
    val today = texts.getString(R.string.today)
    val dateStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = look.accent, textAlign = look.startAlign)
    val titleSize = if (style == HeaderStyle.Full) 20.sp else 17.sp

    val children = mutableListOf<@Composable RowScope.() -> Unit>()
    children += {
        if (style == HeaderStyle.Inline) {
            FlipRow(look, GlanceModifier.defaultWeight(), listOf(
                { Text(today, style = TextStyle(fontSize = titleSize, fontWeight = FontWeight.Bold, color = look.primary), maxLines = 1) },
                { Spacer(GlanceModifier.width(6.dp)) },
                { Text(date, style = dateStyle, maxLines = 1) },
            ))
        } else {
            Column(GlanceModifier.defaultWeight(), horizontalAlignment = look.startColumn) {
                Text(date, style = dateStyle, maxLines = 1)
                Text(today, style = TextStyle(fontSize = titleSize, fontWeight = FontWeight.Bold, color = look.primary, textAlign = look.startAlign), maxLines = 1)
            }
        }
    }
    if (state.totalCount > 0) {
        children += {
            Text(
                "${state.doneCount}/${state.totalCount}",
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = look.secondary),
            )
        }
    }
    if (style != HeaderStyle.Compact) {
        children += { Spacer(GlanceModifier.width(8.dp)) }
        children += {
            Box(
                GlanceModifier
                    .size(26.dp)
                    .background(ImageProvider(R.drawable.widget_circle), colorFilter = ColorFilter.tint(look.accent))
                    .clickable(openApp(context, "new")),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    ImageProvider(R.drawable.ic_add),
                    contentDescription = texts.getString(R.string.new_task),
                    colorFilter = ColorFilter.tint(look.white),
                    modifier = GlanceModifier.size(16.dp),
                )
            }
        }
    }
    FlipRow(look, GlanceModifier.fillMaxWidth(), children)
}

/** One task with a check circle that works right inside the widget. */
@Composable
private fun TaskLine(task: WidgetTask, look: WidgetLook, compact: Boolean) {
    val priorityColor = look.priority(task.priority)
    val children = mutableListOf<@Composable RowScope.() -> Unit>()
    children += {
        Box(
            GlanceModifier
                .size(if (compact) 24.dp else 28.dp)
                .clickable(actionRunCallback<ToggleTaskAction>(actionParametersOf(ToggleTaskAction.TaskId to task.id))),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                ImageProvider(if (task.isDone) R.drawable.widget_checked else R.drawable.widget_ring),
                contentDescription = null,
                colorFilter = ColorFilter.tint(if (task.isDone) look.accent else priorityColor ?: look.secondary),
                modifier = GlanceModifier.size(if (compact) 17.dp else 20.dp),
            )
        }
    }
    children += { Spacer(GlanceModifier.width(6.dp)) }
    children += {
        Text(
            task.title,
            style = TextStyle(
                fontSize = if (compact) 12.sp else 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (task.isDone) look.secondary else look.primary,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                textAlign = look.startAlign,
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
    }
    if (!task.isDone && task.time != null) {
        children += {
            Text(
                task.time,
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (task.isLate) look.late else look.secondary),
                maxLines = 1,
            )
        }
    } else if (!task.isDone && priorityColor != null) {
        children += {
            Text(task.priority.marks, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = priorityColor))
        }
    }
    FlipRow(look, GlanceModifier.fillMaxWidth(), children)
}

@Composable
private fun EmptyState(state: WidgetState, look: WidgetLook, texts: Context) {
    Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
        Image(
            ImageProvider(if (state.totalCount > 0) R.drawable.ic_verified else R.drawable.ic_sun_fill),
            contentDescription = null,
            colorFilter = ColorFilter.tint(look.accent),
            modifier = GlanceModifier.size(26.dp),
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(
            texts.getString(if (state.totalCount > 0) R.string.all_done else R.string.no_tasks_today),
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = look.primary, textAlign = TextAlign.Center),
        )
    }
}
