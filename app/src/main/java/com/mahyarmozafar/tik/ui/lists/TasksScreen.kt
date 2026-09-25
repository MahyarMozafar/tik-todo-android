package com.mahyarmozafar.tik.ui.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.app.AppData
import com.mahyarmozafar.tik.logic.DayGroup
import com.mahyarmozafar.tik.logic.TaskFilter
import com.mahyarmozafar.tik.model.SmartList
import com.mahyarmozafar.tik.ui.LocalAppData
import com.mahyarmozafar.tik.ui.LocalModel
import com.mahyarmozafar.tik.ui.components.AppBackground
import com.mahyarmozafar.tik.ui.components.EmptyState
import com.mahyarmozafar.tik.ui.components.GlassIconButton
import com.mahyarmozafar.tik.ui.components.GlassMenu
import com.mahyarmozafar.tik.ui.components.GlassTopBar
import com.mahyarmozafar.tik.ui.components.LocalSwipeGroup
import com.mahyarmozafar.tik.ui.components.MenuItem
import com.mahyarmozafar.tik.ui.components.QuickAddBar
import com.mahyarmozafar.tik.ui.components.SwipeGroup
import com.mahyarmozafar.tik.ui.components.TaskHandlers
import com.mahyarmozafar.tik.ui.components.TaskItem
import com.mahyarmozafar.tik.ui.components.drawable
import com.mahyarmozafar.tik.ui.components.rememberNow
import com.mahyarmozafar.tik.ui.components.title
import com.mahyarmozafar.tik.ui.glass.LocalBackdrop
import com.mahyarmozafar.tik.ui.navigation.Place
import com.mahyarmozafar.tik.ui.navigation.Route
import com.mahyarmozafar.tik.ui.navigation.navigator
import com.mahyarmozafar.tik.ui.rememberTaskHandlers
import com.mahyarmozafar.tik.ui.theme.ProvideAccent
import com.mahyarmozafar.tik.ui.theme.TikTheme
import com.mahyarmozafar.tik.ui.theme.system
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.time.Instant

/** The tasks in one list, or in a smart list like Scheduled. */
@Composable
fun TasksScreen(place: Place, modifier: Modifier = Modifier, showBackButton: Boolean = true) {
    val data = LocalAppData.current
    val navigator = navigator
    val list = (place as? Place.Custom)?.let { data.list(it.listId) }
    if (place is Place.Custom && list == null) {
        // The list was deleted.
        LaunchedEffect(Unit) { navigator.back() }
        return
    }
    val accent = list?.color?.system?.color(TikTheme.colors.dark) ?: TikTheme.colors.accent
    val backdrop = rememberHazeState()
    ProvideAccent(accent) {
        CompositionLocalProvider(LocalBackdrop provides backdrop, LocalSwipeGroup provides remember { SwipeGroup() }) {
            TasksContent(place, data, modifier, showBackButton)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TasksContent(place: Place, data: AppData, modifier: Modifier, showBackButton: Boolean) {
    val model = LocalModel.current
    val navigator = navigator
    val backdrop = LocalBackdrop.current
    val handlers = rememberTaskHandlers()
    val density = LocalDensity.current
    val colors = TikTheme.colors
    val settings = TikTheme.settings
    val formatting = TikTheme.formatting
    val calendar = formatting.calendar
    val now = rememberNow()
    val list = (place as? Place.Custom)?.let { data.list(it.listId) }

    var headerHeight by remember { mutableIntStateOf(0) }
    var bottomHeight by remember { mutableIntStateOf(0) }
    var showCompleted by rememberSaveable { mutableStateOf(false) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val title = when (place) {
        is Place.Smart -> stringResource(place.list.title)
        Place.Inbox -> stringResource(R.string.inbox)
        is Place.Custom -> list?.name.orEmpty()
    }
    val icon = when (place) {
        is Place.Smart -> place.list.drawable
        Place.Inbox -> R.drawable.ic_inbox_fill
        is Place.Custom -> list?.icon?.drawable ?: R.drawable.ic_list_list
    }
    val canAdd = place != Place.Smart(SmartList.Completed)

    /** Where a task added here goes. */
    fun newTaskDefaults(): Pair<Instant?, String?> {
        val today = calendar.startOfDay(Instant.now())
        return when (place) {
            Place.Smart(SmartList.Today) -> today to null
            Place.Smart(SmartList.Scheduled) -> calendar.addDays(today, 1) to null
            is Place.Custom -> null to place.listId
            else -> null to null
        }
    }

    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().hazeSource(backdrop)) {
            AppBackground()
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("tasksList"),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = with(density) { headerHeight.toDp() } + 4.dp,
                    bottom = with(density) { bottomHeight.toDp() } + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (place) {
                    Place.Smart(SmartList.Scheduled) -> groups(
                        TaskFilter.scheduled(data.tasks, now, calendar, settings.sortOrder), data, now, handlers, showsDay = false,
                    )
                    Place.Smart(SmartList.Completed) -> groups(TaskFilter.completed(data.tasks, calendar), data, now, handlers, showsDay = true)
                    else -> {
                        val scoped = when (place) {
                            Place.Smart(SmartList.Today) -> TaskFilter.today(data.tasks, now, calendar, settings.sortOrder)
                            Place.Inbox -> data.tasks.filter { it.listId == null }
                            is Place.Custom -> data.tasks.filter { it.listId == place.listId }
                            else -> data.tasks
                        }
                        val open = TaskFilter.sorted(scoped.filter { !it.isDone }, settings.sortOrder, now, calendar)
                        val done = scoped.filter { it.isDone }.sortedByDescending { it.completedAt ?: Instant.MIN }
                        val showsList = place !is Place.Custom
                        val showsDay = place != Place.Smart(SmartList.Today)
                        items(open, key = { it.id }) { task ->
                            TaskItem(task, data.list(task.listId), now, handlers, Modifier.animateItem(), showsDay = showsDay, showsList = showsList)
                        }
                        if (done.isNotEmpty()) {
                            item(key = "completedToggle") {
                                CompletedToggle(showCompleted, done.size, Modifier.animateItem()) { showCompleted = !showCompleted }
                            }
                            if (showCompleted) {
                                items(done, key = { it.id }) { task ->
                                    TaskItem(task, data.list(task.listId), now, handlers, Modifier.animateItem(), showsList = showsList)
                                }
                            }
                        }
                        if (open.isEmpty() && done.isEmpty()) emptyState(icon, place)
                    }
                }
                if (place == Place.Smart(SmartList.Scheduled) && TaskFilter.scheduled(data.tasks, now, calendar, settings.sortOrder).isEmpty()) {
                    emptyState(icon, place)
                }
                if (place == Place.Smart(SmartList.Completed) && data.tasks.none { it.isDone }) {
                    emptyState(icon, place)
                }
            }
        }

        GlassTopBar(backdrop, Modifier.onSizeChanged { headerHeight = it.height }) {
            if (showBackButton) {
                GlassIconButton(
                    icon = R.drawable.ic_arrow_back,
                    contentDescription = stringResource(R.string.back),
                    backdrop = backdrop,
                    onClick = { navigator.back() },
                    modifier = Modifier.testTag("backButton"),
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(
                title,
                style = TikTheme.type.title,
                color = colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (list != null) {
                Box {
                    GlassIconButton(
                        icon = R.drawable.ic_more,
                        contentDescription = stringResource(R.string.more),
                        backdrop = backdrop,
                        onClick = { menuOpen = true },
                        modifier = Modifier.testTag("listMenuButton"),
                    )
                    GlassMenu(expanded = menuOpen, onDismiss = { menuOpen = false }) {
                        MenuItem(stringResource(R.string.edit_list), R.drawable.ic_edit) {
                            menuOpen = false
                            navigator.open(Route.ListEditor(list.id))
                        }
                        MenuItem(stringResource(R.string.delete_list), R.drawable.ic_delete, tint = colors.destructive) {
                            menuOpen = false
                            confirmDelete = true
                        }
                    }
                }
            }
        }

        if (canAdd) {
            QuickAddBar(
                expanded = adding,
                onExpandedChange = { adding = it },
                backdrop = backdrop,
                onAdd = { text ->
                    val (due, listId) = newTaskDefaults()
                    model.launch { addTask(text, due, listId) }
                },
                onShowDetails = { text ->
                    val (due, listId) = newTaskDefaults()
                    navigator.open(Route.Editor(title = text, dueDateMillis = due?.toEpochMilli(), listId = listId))
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { bottomHeight = it.height }
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(bottom = 4.dp),
            )
        }
    }

    if (confirmDelete && list != null) {
        DeleteListDialog(
            list = list,
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                // Leave the screen first, so it never shows a list that is gone.
                navigator.back()
                model.launch { deleteList(list) }
            },
        )
    }
}

private fun LazyListScope.groups(groups: List<DayGroup>, data: AppData, now: Instant, handlers: TaskHandlers, showsDay: Boolean) {
    for (group in groups) {
        item(key = "day-${group.day.toEpochMilli()}") {
            DayHeader(group.day, now, Modifier.animateItem())
        }
        items(group.tasks, key = { it.id }) { task ->
            TaskItem(task, data.list(task.listId), now, handlers, Modifier.animateItem(), showsDay = showsDay)
        }
    }
}

private fun LazyListScope.emptyState(icon: Int, place: Place) {
    item(key = "empty") {
        EmptyState(
            icon = icon,
            title = stringResource(R.string.nothing_here_yet),
            message = stringResource(
                if (place == Place.Smart(SmartList.Completed)) R.string.finished_tasks_show_here else R.string.tap_plus_to_add,
            ),
            modifier = Modifier.animateItem(),
        )
    }
}

@Composable
private fun DayHeader(day: Instant, now: Instant, modifier: Modifier = Modifier) {
    val formatting = TikTheme.formatting
    val relative = formatting.relativeDay(day, now)
    val detail = formatting.dayAndMonth(day)
    Row(modifier.fillMaxWidth().padding(start = 6.dp, top = 14.dp, bottom = 2.dp), verticalAlignment = Alignment.Bottom) {
        Text(relative, style = TikTheme.type.headline.copy(fontWeight = FontWeight.Bold), color = TikTheme.colors.primaryText)
        if (relative != detail) {
            Spacer(Modifier.width(8.dp))
            Text(detail, style = TikTheme.type.subheadline, color = TikTheme.colors.secondaryText)
        }
    }
}

@Composable
private fun CompletedToggle(open: Boolean, count: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = TikTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(start = 6.dp, end = 6.dp, top = 14.dp, bottom = 6.dp)
            .testTag("completedToggle"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (open) stringResource(R.string.hide_completed) else stringResource(R.string.show_completed, count.toString()),
            style = TikTheme.type.subheadline.copy(fontWeight = FontWeight.SemiBold),
            color = colors.secondaryText,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painterResource(R.drawable.ic_expand),
            contentDescription = null,
            tint = colors.secondaryText,
            modifier = Modifier.size(20.dp).rotate(if (open) 180f else 0f),
        )
    }
}
