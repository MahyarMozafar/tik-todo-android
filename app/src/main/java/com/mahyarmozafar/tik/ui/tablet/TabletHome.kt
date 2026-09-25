package com.mahyarmozafar.tik.ui.tablet

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.logic.TaskFilter
import com.mahyarmozafar.tik.model.SmartList
import com.mahyarmozafar.tik.model.TaskList
import com.mahyarmozafar.tik.model.TaskSortOrder
import com.mahyarmozafar.tik.ui.LocalAppData
import com.mahyarmozafar.tik.ui.LocalModel
import com.mahyarmozafar.tik.ui.components.AppBackground
import com.mahyarmozafar.tik.ui.components.GlassIconButton
import com.mahyarmozafar.tik.ui.components.GlassMenu
import com.mahyarmozafar.tik.ui.components.IconBadge
import com.mahyarmozafar.tik.ui.components.MenuItem
import com.mahyarmozafar.tik.ui.components.drawable
import com.mahyarmozafar.tik.ui.components.rememberFeedback
import com.mahyarmozafar.tik.ui.components.rememberNow
import com.mahyarmozafar.tik.ui.components.title
import com.mahyarmozafar.tik.ui.glass.CapsuleShape
import com.mahyarmozafar.tik.ui.glass.LocalBackdrop
import com.mahyarmozafar.tik.ui.glass.card
import com.mahyarmozafar.tik.ui.lists.DeleteListDialog
import com.mahyarmozafar.tik.ui.lists.TasksScreen
import com.mahyarmozafar.tik.ui.navigation.Place
import com.mahyarmozafar.tik.ui.navigation.Route
import com.mahyarmozafar.tik.ui.navigation.navigator
import com.mahyarmozafar.tik.ui.search.SearchScreen
import com.mahyarmozafar.tik.ui.theme.SystemColor
import com.mahyarmozafar.tik.ui.theme.TikTheme
import com.mahyarmozafar.tik.ui.theme.color
import com.mahyarmozafar.tik.ui.theme.system
import com.mahyarmozafar.tik.ui.today.TodayScreen
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.serialization.json.Json

/**
 * The tablet layout: a floating sidebar with search, Today, the smart lists and your own lists,
 * and the chosen one next to it. Like the iPad version.
 */
@Composable
fun TabletHome() {
    val data = LocalAppData.current
    val model = LocalModel.current

    // Null means Today. Kept as text so it survives turning the tablet.
    var selectedText by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = selectedText?.let { Json.decodeFromString(Place.serializer(), it) }
    fun select(place: Place?) {
        selectedText = place?.let { Json.encodeToString(Place.serializer(), it) }
    }
    var query by rememberSaveable { mutableStateOf("") }
    var startAdding by remember { mutableStateOf(false) }

    // Go back to Today if the open list was deleted.
    LaunchedEffect(selected, data.lists) {
        if (selected is Place.Custom && data.list(selected.listId) == null) select(null)
    }
    // The widget's + button opens Today with the quick add field ready.
    val pendingQuickAdd by model.pendingQuickAdd.collectAsStateWithLifecycle()
    LaunchedEffect(pendingQuickAdd) {
        if (pendingQuickAdd) {
            model.pendingQuickAdd.value = false
            query = ""
            select(null)
            startAdding = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        AppBackground()
        Row(Modifier.fillMaxSize()) {
            Sidebar(
                selected = selected,
                query = query,
                onQueryChange = { query = it },
                onSelect = {
                    query = ""
                    select(it)
                },
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(12.dp),
            )
            Box(Modifier.weight(1f).fillMaxHeight()) {
                val backdrop = rememberHazeState()
                CompositionLocalProvider(LocalBackdrop provides backdrop) {
                    val detail: Any = if (query.isNotBlank()) "search" else selected ?: "today"
                    AnimatedContent(
                        targetState = detail,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "detail",
                    ) { shown ->
                        when (shown) {
                            "search" -> SearchScreen(query = query, bottomPadding = 0.dp)
                            is Place -> TasksScreen(shown, showBackButton = false)
                            else -> TodayScreen(
                                bottomPadding = 0.dp,
                                showSettingsButton = false,
                                ownQuickAdd = true,
                                startAdding = startAdding,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Sidebar(
    selected: Place?,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (Place?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val data = LocalAppData.current
    val model = LocalModel.current
    val navigator = navigator
    val colors = TikTheme.colors
    val type = TikTheme.type
    val backdrop = rememberHazeState()
    val now = rememberNow()
    val calendar = TikTheme.formatting.calendar
    var toDelete by remember { mutableStateOf<TaskList?>(null) }

    val todayCount = remember(data.tasks, now) { TaskFilter.today(data.tasks, now, calendar, TaskSortOrder.Time).count { !it.isDone } }
    val openCounts = remember(data.tasks) { data.tasks.filter { !it.isDone }.groupingBy { it.listId }.eachCount() }

    LazyColumn(
        modifier.card(RoundedCornerShape(28.dp)),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(key = "title") {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.app_name), style = type.title, color = colors.primaryText, modifier = Modifier.weight(1f))
                GlassIconButton(
                    icon = R.drawable.ic_settings,
                    contentDescription = stringResource(R.string.settings),
                    backdrop = backdrop,
                    onClick = { navigator.open(Route.Settings) },
                    size = 40.dp,
                    modifier = Modifier.testTag("settingsButton"),
                )
                Spacer(Modifier.width(8.dp))
                GlassIconButton(
                    icon = R.drawable.ic_add,
                    contentDescription = stringResource(R.string.new_list),
                    backdrop = backdrop,
                    onClick = { navigator.open(Route.ListEditor(null)) },
                    size = 40.dp,
                )
            }
        }
        item(key = "search") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(colors.fill, CapsuleShape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = type.body.copy(color = colors.primaryText),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.weight(1f).testTag("searchField"),
                    decorationBox = { field ->
                        Box {
                            if (query.isEmpty()) {
                                Text(stringResource(R.string.search_prompt), style = type.body, color = colors.tertiaryText, maxLines = 1)
                            }
                            field()
                        }
                    },
                )
            }
        }
        item(key = "today") {
            SidebarRow(
                title = stringResource(R.string.today),
                icon = R.drawable.ic_sun_fill,
                color = SmartList.Today.color(colors.dark),
                count = todayCount,
                selected = selected == null && query.isBlank(),
                onClick = { onSelect(null) },
            )
        }
        items(listOf(SmartList.Scheduled, SmartList.All, SmartList.Completed), key = { it.name }) { smart ->
            val place = Place.Smart(smart)
            SidebarRow(
                title = stringResource(smart.title),
                icon = smart.drawable,
                color = smart.color(colors.dark),
                count = 0,
                selected = selected == place && query.isBlank(),
                onClick = { onSelect(place) },
            )
        }
        item(key = "myLists") {
            Text(
                stringResource(R.string.my_lists),
                style = type.footnote.copy(fontWeight = FontWeight.SemiBold),
                color = colors.secondaryText,
                modifier = Modifier.padding(start = 12.dp, top = 18.dp, bottom = 6.dp),
            )
        }
        item(key = "inbox") {
            SidebarRow(
                title = stringResource(R.string.inbox),
                icon = R.drawable.ic_inbox_fill,
                color = SystemColor.Gray.color(colors.dark),
                count = openCounts[null] ?: 0,
                selected = selected == Place.Inbox && query.isBlank(),
                onClick = { onSelect(Place.Inbox) },
            )
        }
        items(data.lists, key = { it.id }) { list ->
            val place = Place.Custom(list.id)
            var menuOpen by remember { mutableStateOf(false) }
            val feedback = rememberFeedback()
            Box {
                SidebarRow(
                    title = list.name,
                    icon = list.icon.drawable,
                    color = list.color.system.color(colors.dark),
                    count = openCounts[list.id] ?: 0,
                    selected = selected == place && query.isBlank(),
                    onClick = { onSelect(place) },
                    onLongClick = {
                        feedback.longPress()
                        menuOpen = true
                    },
                )
                GlassMenu(expanded = menuOpen, onDismiss = { menuOpen = false }) {
                    MenuItem(stringResource(R.string.edit_list), R.drawable.ic_edit) {
                        menuOpen = false
                        navigator.open(Route.ListEditor(list.id))
                    }
                    MenuItem(stringResource(R.string.delete_list), R.drawable.ic_delete, tint = colors.destructive) {
                        menuOpen = false
                        toDelete = list
                    }
                }
            }
        }
        item(key = "bottom") { Spacer(Modifier.height(12.dp)) }
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

/** A sidebar row. The chosen one is filled with the accent color, like the iPad sidebar. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SidebarRow(
    title: String,
    @DrawableRes icon: Int,
    color: Color,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = TikTheme.colors
    val type = TikTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) colors.accent else Color.Transparent, RoundedCornerShape(14.dp))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics { this.selected = selected }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, if (selected) Color.White.copy(alpha = 0.25f) else color, size = 30.dp, iconSize = 16.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = type.body.copy(fontWeight = FontWeight.Medium),
            color = if (selected) colors.onAccent else colors.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (count > 0) {
            Text(count.toString(), style = type.subheadline, color = if (selected) colors.onAccent else colors.secondaryText)
        }
    }
}
