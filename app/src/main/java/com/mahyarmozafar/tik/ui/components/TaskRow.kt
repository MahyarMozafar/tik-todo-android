package com.mahyarmozafar.tik.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.logic.TaskFilter
import com.mahyarmozafar.tik.model.Priority
import com.mahyarmozafar.tik.model.Task
import com.mahyarmozafar.tik.model.TaskList
import com.mahyarmozafar.tik.ui.glass.card
import com.mahyarmozafar.tik.ui.theme.Motion
import com.mahyarmozafar.tik.ui.theme.TikTheme
import com.mahyarmozafar.tik.ui.theme.color
import com.mahyarmozafar.tik.ui.theme.system
import kotlinx.coroutines.delay
import java.time.Instant

/** What can be done to a task from a list: tap, swipe or the long-press menu. */
class TaskHandlers(
    val toggle: (Task) -> Unit,
    val edit: (Task) -> Unit,
    val delete: (Task) -> Unit,
    val moveToTomorrow: (Task) -> Unit,
    val duplicate: (Task) -> Unit,
    val setPriority: (Task, Priority) -> Unit,
)

/**
 * One task in a list: the card, plus tap to edit, swipe to tick, delete or move to tomorrow, and
 * a long-press menu with more.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    task: Task,
    list: TaskList?,
    now: Instant,
    handlers: TaskHandlers,
    modifier: Modifier = Modifier,
    showsDay: Boolean = true,
    showsList: Boolean = true,
) {
    val colors = TikTheme.colors
    val feedback = rememberFeedback()
    var menuOpen by remember { mutableStateOf(false) }
    // Glass rows don't ripple; they sink in a little while pressed, like on iOS.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed || menuOpen) 0.975f else 1f, Motion.fastSpatial(), label = "press")

    val done = SwipeAction(
        icon = if (task.isDone) R.drawable.ic_undo else R.drawable.ic_check,
        label = stringResource(if (task.isDone) R.string.not_done else R.string.done),
        color = colors.success,
    ) {
        feedback.tick(done = !task.isDone)
        handlers.toggle(task)
    }
    val delete = SwipeAction(R.drawable.ic_delete, stringResource(R.string.delete), colors.destructive) {
        handlers.delete(task)
    }
    val tomorrow = SwipeAction(R.drawable.ic_tomorrow, stringResource(R.string.tomorrow), colors.warning) {
        handlers.moveToTomorrow(task)
    }

    Box(modifier) {
        SwipeActionsBox(
            key = task.id,
            leading = done,
            trailing = if (task.isDone) listOf(delete) else listOf(delete, tomorrow),
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        ) {
            TaskRow(
                task = task,
                list = list,
                now = now,
                showsDay = showsDay,
                showsList = showsList,
                onToggle = { handlers.toggle(task) },
                modifier = Modifier.combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = { handlers.edit(task) },
                    onLongClick = {
                        feedback.longPress()
                        menuOpen = true
                    },
                ),
            )
        }
        TaskMenu(task, expanded = menuOpen, onDismiss = { menuOpen = false }, handlers = handlers)
    }
}

/** The task card: the check circle, the title, and a line of small details (time, list...). */
@Composable
fun TaskRow(
    task: Task,
    list: TaskList?,
    now: Instant,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    showsDay: Boolean = true,
    showsList: Boolean = true,
) {
    val colors = TikTheme.colors
    val type = TikTheme.type
    val fields = TikTheme.settings.fields
    val feedback = rememberFeedback()

    // The tick shows right away. The real change comes a moment later, so the check animation
    // can play before the task moves down the list.
    var pendingDone by remember(task.id) { mutableStateOf<Boolean?>(null) }
    val isDone = pendingDone ?: task.isDone
    LaunchedEffect(pendingDone) {
        val target = pendingDone ?: return@LaunchedEffect
        delay(450)
        if (task.isDone != target) onToggle() else pendingDone = null
    }
    // Saving takes a moment; keep showing the tick until the saved task agrees.
    LaunchedEffect(task.isDone) {
        if (pendingDone == task.isDone) pendingDone = null
    }

    val priorityColor = if (fields.priority) task.priority.color(colors.dark) else null
    val markDone = stringResource(R.string.mark_as_done)
    val markNotDone = stringResource(R.string.mark_as_not_done)

    Row(
        modifier
            .fillMaxWidth()
            .card()
            .alpha(if (isDone) 0.7f else 1f)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .padding(top = 1.dp)
                .toggleable(
                    value = isDone,
                    role = Role.Checkbox,
                    onValueChange = { target ->
                        feedback.tick(done = target)
                        pendingDone = target
                    },
                )
                .semantics { contentDescription = if (isDone) markNotDone else markDone }
                .testTag("check-${task.title}"),
        ) {
            CheckCircle(isDone = isDone, ringColor = priorityColor ?: colors.secondaryText)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                task.title,
                style = type.body.copy(
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (isDone) TextDecoration.LineThrough else null,
                ),
                color = if (isDone) colors.secondaryText else colors.primaryText,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("taskTitle"),
            )
            TaskDetails(task, list, now, showsDay, showsList)
        }
        if (priorityColor != null && !isDone) {
            Spacer(Modifier.width(8.dp))
            val name = stringResource(task.priority.title)
            Text(
                task.priority.marks,
                style = type.subheadline.copy(fontWeight = FontWeight.Black),
                color = priorityColor,
                modifier = Modifier.clearAndSetSemantics { contentDescription = name },
            )
        }
    }
}

@Composable
private fun TaskDetails(task: Task, list: TaskList?, now: Instant, showsDay: Boolean, showsList: Boolean) {
    val colors = TikTheme.colors
    val formatting = TikTheme.formatting
    val fields = TikTheme.settings.fields
    val calendar = formatting.calendar
    val due = task.dueDate

    val overdue = TaskFilter.isOverdue(task, now, calendar)
    val showsDue = fields.dates && due != null && (showsDay || task.hasTime || overdue)
    val showsRepeat = fields.repeat && task.repeatRule != null
    val showsListName = showsList && list != null
    val showsSubtasks = fields.subtasks && task.subtasks.isNotEmpty()
    val showsNote = fields.notes && task.note.isNotEmpty()
    val showsPhoto = fields.photos && task.hasPhoto
    if (!(showsDue || showsRepeat || showsListName || showsSubtasks || showsNote || showsPhoto)) return

    val style = TikTheme.type.caption.copy(fontWeight = FontWeight.Medium)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showsDue && due != null) {
            val day = if (showsDay || overdue) formatting.relativeDay(due, now) else null
            val time = if (task.hasTime) formatting.time(due) else null
            val late = TaskFilter.isLate(task, now, calendar)
            val tint = if (late) colors.destructive else colors.secondaryText
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(if (task.hasTime) R.drawable.ic_clock else R.drawable.ic_calendar),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(listOfNotNull(day, time).joinToString(" · "), style = style, color = tint, maxLines = 1)
            }
        }
        if (showsRepeat) DetailIcon(R.drawable.ic_repeat, stringResource(R.string.repeats))
        if (showsListName && list != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(list.color.system.color(colors.dark), CircleShape))
                Spacer(Modifier.width(4.dp))
                Text(list.name, style = style, color = colors.secondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (showsSubtasks) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DetailIcon(R.drawable.ic_checklist, null)
                Spacer(Modifier.width(3.dp))
                Text("${task.doneSubtaskCount}/${task.subtasks.size}", style = style, color = colors.secondaryText)
            }
        }
        if (showsNote) DetailIcon(R.drawable.ic_notes, stringResource(R.string.notes))
        if (showsPhoto) DetailIcon(R.drawable.ic_photo, stringResource(R.string.photo))
    }
}

@Composable
private fun DetailIcon(icon: Int, description: String?) {
    Icon(
        painterResource(icon),
        contentDescription = description,
        tint = TikTheme.colors.secondaryText,
        modifier = Modifier.size(13.dp),
    )
}

/** The long-press menu: edit, tick, move to tomorrow, priority, duplicate and delete. */
@Composable
fun TaskMenu(task: Task, expanded: Boolean, onDismiss: () -> Unit, handlers: TaskHandlers) {
    val colors = TikTheme.colors
    val feedback = rememberFeedback()
    var showPriorities by remember(expanded) { mutableStateOf(false) }

    GlassMenu(expanded = expanded, onDismiss = onDismiss, offset = DpOffset(16.dp, 0.dp)) {
        if (showPriorities) {
            for (priority in Priority.entries) {
                MenuItem(
                    text = stringResource(priority.title),
                    icon = if (priority == task.priority) R.drawable.ic_check else null,
                    tint = priority.color(colors.dark),
                ) {
                    handlers.setPriority(task, priority)
                    onDismiss()
                }
            }
            return@GlassMenu
        }
        MenuItem(stringResource(R.string.edit), R.drawable.ic_edit) {
            onDismiss()
            handlers.edit(task)
        }
        MenuItem(
            stringResource(if (task.isDone) R.string.mark_as_not_done else R.string.mark_as_done),
            if (task.isDone) R.drawable.ic_undo else R.drawable.ic_check,
        ) {
            onDismiss()
            feedback.tick(done = !task.isDone)
            handlers.toggle(task)
        }
        if (!task.isDone) {
            MenuItem(stringResource(R.string.move_to_tomorrow), R.drawable.ic_tomorrow) {
                onDismiss()
                handlers.moveToTomorrow(task)
            }
        }
        MenuItem(stringResource(R.string.priority), R.drawable.ic_priority, trailing = R.drawable.ic_chevron_end) {
            showPriorities = true
        }
        MenuItem(stringResource(R.string.duplicate), R.drawable.ic_duplicate) {
            onDismiss()
            handlers.duplicate(task)
        }
        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.separator)
        MenuItem(stringResource(R.string.delete), R.drawable.ic_delete, tint = colors.destructive) {
            onDismiss()
            handlers.delete(task)
        }
    }
}

/** A rounded, almost see-through menu, used for every long-press and "more" menu. */
@Composable
fun GlassMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    content: @Composable () -> Unit,
) {
    val colors = TikTheme.colors
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier.padding(vertical = 2.dp),
        offset = offset,
        shape = RoundedCornerShape(20.dp),
        containerColor = colors.glassFallback,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
    ) {
        content()
    }
}

@Composable
fun MenuItem(
    text: String,
    icon: Int? = null,
    tint: Color? = null,
    trailing: Int? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = TikTheme.colors
    val color = tint ?: colors.primaryText
    DropdownMenuItem(
        text = { Text(text, style = TikTheme.type.body, color = if (tint != null && icon != null) color else colors.primaryText) },
        onClick = onClick,
        enabled = enabled,
        leadingIcon = icon?.let {
            { Icon(painterResource(it), contentDescription = null, tint = color, modifier = Modifier.size(20.dp)) }
        },
        trailingIcon = trailing?.let {
            { Icon(painterResource(it), contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(18.dp)) }
        },
    )
}
