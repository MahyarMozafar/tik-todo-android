package com.mahyarmozafar.tik.ui.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.logic.TaskFilter
import com.mahyarmozafar.tik.model.SmartList
import com.mahyarmozafar.tik.model.Task
import com.mahyarmozafar.tik.model.TaskList
import com.mahyarmozafar.tik.model.TaskSortOrder
import com.mahyarmozafar.tik.time.TikCalendar
import com.mahyarmozafar.tik.ui.LocalAppData
import com.mahyarmozafar.tik.ui.LocalModel
import com.mahyarmozafar.tik.ui.components.AppBackground
import com.mahyarmozafar.tik.ui.components.GlassIconButton
import com.mahyarmozafar.tik.ui.components.GlassMenu
import com.mahyarmozafar.tik.ui.components.GlassTopBar
import com.mahyarmozafar.tik.ui.components.GroupRow
import com.mahyarmozafar.tik.ui.components.IconBadge
import com.mahyarmozafar.tik.ui.components.ListRowContent
import com.mahyarmozafar.tik.ui.components.MenuItem
import com.mahyarmozafar.tik.ui.components.drawable
import com.mahyarmozafar.tik.ui.components.groupPosition
import com.mahyarmozafar.tik.ui.components.pressScale
import com.mahyarmozafar.tik.ui.components.rememberFeedback
import com.mahyarmozafar.tik.ui.components.rememberNow
import com.mahyarmozafar.tik.ui.components.title
import com.mahyarmozafar.tik.ui.glass.LocalBackdrop
import com.mahyarmozafar.tik.ui.glass.card
import com.mahyarmozafar.tik.ui.navigation.Place
import com.mahyarmozafar.tik.ui.navigation.Route
import com.mahyarmozafar.tik.ui.navigation.navigator
import com.mahyarmozafar.tik.ui.theme.SystemColor
import com.mahyarmozafar.tik.ui.theme.TikTheme
import com.mahyarmozafar.tik.ui.theme.color
import com.mahyarmozafar.tik.ui.theme.system
import dev.chrisbanes.haze.hazeSource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.Instant

/** The Lists tab: four smart lists at the top, then the Inbox and the user's own lists. */
@Composable
fun ListsScreen(bottomPadding: Dp, modifier: Modifier = Modifier) {
    val data = LocalAppData.current
    val model = LocalModel.current
    val backdrop = LocalBackdrop.current
    val navigator = navigator
    val density = LocalDensity.current
    val feedback = rememberFeedback()
    val now = rememberNow()
    val calendar = TikTheme.formatting.calendar

    var ordered by remember(data.lists) { mutableStateOf(data.lists) }
    var toDelete by remember { mutableStateOf<TaskList?>(null) }
    var headerHeight by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = ordered.indexOfFirst { it.id == from.key }
        val toIndex = ordered.indexOfFirst { it.id == to.key }
        if (fromIndex >= 0 && toIndex >= 0) {
            ordered = ordered.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            feedback.selection()
        }
    }
    val openCounts = remember(data.tasks) { data.tasks.filter { !it.isDone }.groupingBy { it.listId }.eachCount() }

    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().hazeSource(backdrop)) {
            AppBackground()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag("listsList"),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = with(density) { headerHeight.toDp() } + 4.dp,
                    bottom = bottomPadding + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item(key = "smart") {
                    SmartGrid(counts = smartCounts(data.tasks, now, calendar), onOpen = { navigator.open(Route.Tasks(Place.Smart(it))) })
                }
                item(key = "header") {
                    Text(
                        stringResource(R.string.my_lists),
                        style = TikTheme.type.title3.copy(fontWeight = FontWeight.Bold),
                        color = TikTheme.colors.primaryText,
                        modifier = Modifier.padding(start = 6.dp, top = 22.dp, bottom = 8.dp),
                    )
                }
                item(key = "inbox") {
                    GroupRow(
                        position = groupPosition(0, ordered.size + 1),
                        onClick = { navigator.open(Route.Tasks(Place.Inbox)) },
                        modifier = Modifier.testTag("inboxRow"),
                    ) {
                        ListRowContent(
                            title = stringResource(R.string.inbox),
                            icon = R.drawable.ic_inbox_fill,
                            color = SystemColor.Gray.color(TikTheme.colors.dark),
                            count = openCounts[null] ?: 0,
                        )
                    }
                }
                items(ordered, key = { it.id }) { list ->
                    ReorderableItem(reorderState, key = list.id) { dragging ->
                        var menuOpen by remember { mutableStateOf(false) }
                        Box(Modifier.scale(if (dragging) 1.03f else 1f)) {
                            GroupRow(
                                position = groupPosition(ordered.indexOf(list) + 1, ordered.size + 1),
                                onClick = { navigator.open(Route.Tasks(Place.Custom(list.id))) },
                                onLongClick = {
                                    feedback.longPress()
                                    menuOpen = true
                                },
                                modifier = Modifier.testTag("list-${list.name}"),
                            ) {
                                ListRowContent(
                                    title = list.name,
                                    icon = list.icon.drawable,
                                    color = list.color.system.color(TikTheme.colors.dark),
                                    count = openCounts[list.id] ?: 0,
                                    trailing = {
                                        val reorder = stringResource(R.string.reorder)
                                        Icon(
                                            painterResource(R.drawable.ic_drag),
                                            contentDescription = reorder,
                                            tint = TikTheme.colors.tertiaryText,
                                            modifier = Modifier
                                                .draggableHandle(
                                                    onDragStarted = { feedback.longPress() },
                                                    onDragStopped = { model.launch { reorderLists(ordered) } },
                                                )
                                                .size(24.dp),
                                        )
                                    },
                                )
                            }
                            GlassMenu(expanded = menuOpen, onDismiss = { menuOpen = false }) {
                                MenuItem(stringResource(R.string.edit_list), R.drawable.ic_edit) {
                                    menuOpen = false
                                    navigator.open(Route.ListEditor(list.id))
                                }
                                MenuItem(stringResource(R.string.delete_list), R.drawable.ic_delete, tint = TikTheme.colors.destructive) {
                                    menuOpen = false
                                    toDelete = list
                                }
                            }
                        }
                    }
                }
            }
        }
        GlassTopBar(backdrop, Modifier.onSizeChanged { headerHeight = it.height }) {
            Text(
                stringResource(R.string.lists),
                style = TikTheme.type.largeTitle,
                color = TikTheme.colors.primaryText,
                modifier = Modifier.weight(1f),
            )
            GlassIconButton(
                icon = R.drawable.ic_add,
                contentDescription = stringResource(R.string.new_list),
                backdrop = backdrop,
                onClick = { navigator.open(Route.ListEditor(null)) },
                modifier = Modifier.testTag("newListButton"),
            )
        }
    }

    toDelete?.let { list ->
        DeleteListDialog(
            list = list,
            onDismiss = { toDelete = null },
            onConfirm = {
                toDelete = null
                model.launch { deleteList(list) }
            },
        )
    }
}

@Composable
fun DeleteListDialog(list: TaskList, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val colors = TikTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_list_named, list.name)) },
        text = { Text(stringResource(R.string.list_tasks_deleted_too)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete_list), color = colors.destructive)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = colors.glassFallback,
    )
}

/** How many tasks each smart list has, the way iOS counts them. */
private fun smartCounts(tasks: List<Task>, now: Instant, calendar: TikCalendar): Map<SmartList, Int> = mapOf(
    SmartList.Today to TaskFilter.today(tasks, now, calendar, TaskSortOrder.Time).count { !it.isDone },
    SmartList.Scheduled to TaskFilter.scheduled(tasks, now, calendar, TaskSortOrder.Time).sumOf { it.tasks.size },
    SmartList.All to tasks.count { !it.isDone },
    SmartList.Completed to tasks.count { it.isDone },
)

@Composable
private fun SmartGrid(counts: Map<SmartList, Int>, onOpen: (SmartList) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SmartList.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { smart ->
                    SmartListCard(smart, counts[smart] ?: 0, onClick = { onOpen(smart) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** A card for a smart list: icon, count and name. */
@Composable
private fun SmartListCard(smart: SmartList, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TikTheme.colors
    val type = TikTheme.type
    val interaction = remember { MutableInteractionSource() }
    val title = stringResource(smart.title)
    Column(
        modifier
            .pressScale(interaction, pressedScale = 0.96f)
            .card(RoundedCornerShape(20.dp))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$title, $count" }
            .testTag("smart-${smart.name.lowercase()}")
            .padding(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(smart.drawable, smart.color(colors.dark), size = 36.dp, iconSize = 19.dp)
            Spacer(Modifier.weight(1f))
            Text(count.toString(), style = type.title, color = colors.primaryText)
        }
        Spacer(Modifier.size(10.dp))
        Text(title, style = type.subheadline.copy(fontWeight = FontWeight.SemiBold), color = colors.secondaryText)
    }
}
