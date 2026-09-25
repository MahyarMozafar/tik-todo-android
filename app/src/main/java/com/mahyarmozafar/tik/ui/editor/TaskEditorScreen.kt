package com.mahyarmozafar.tik.ui.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.TikApplication
import com.mahyarmozafar.tik.logic.SubtaskDraft
import com.mahyarmozafar.tik.logic.TaskDraft
import com.mahyarmozafar.tik.model.Priority
import com.mahyarmozafar.tik.ui.LocalAppData
import com.mahyarmozafar.tik.ui.LocalModel
import com.mahyarmozafar.tik.ui.components.CheckCircle
import com.mahyarmozafar.tik.ui.components.GlassMenu
import com.mahyarmozafar.tik.ui.components.GroupRow
import com.mahyarmozafar.tik.ui.components.IconBadge
import com.mahyarmozafar.tik.ui.components.MenuItem
import com.mahyarmozafar.tik.ui.components.SectionTitle
import com.mahyarmozafar.tik.ui.components.SegmentedControl
import com.mahyarmozafar.tik.ui.components.SheetScaffold
import com.mahyarmozafar.tik.ui.components.drawable
import com.mahyarmozafar.tik.ui.components.groupPosition
import com.mahyarmozafar.tik.ui.components.rememberFeedback
import com.mahyarmozafar.tik.ui.components.title
import com.mahyarmozafar.tik.ui.glass.CapsuleShape
import com.mahyarmozafar.tik.ui.glass.card
import com.mahyarmozafar.tik.ui.navigation.Route
import com.mahyarmozafar.tik.ui.navigation.navigator
import com.mahyarmozafar.tik.ui.theme.Motion
import com.mahyarmozafar.tik.ui.theme.TikTheme
import com.mahyarmozafar.tik.ui.theme.system
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableColumn
import java.time.Instant
import java.time.LocalDate

/**
 * Adds a new task or edits one. Details that are turned off in Settings (notes, subtasks, dates,
 * repeat, priority, photos) are left out.
 */
@Composable
fun TaskEditorScreen(route: Route.Editor) {
    val data = LocalAppData.current
    val model = LocalModel.current
    val navigator = navigator
    val photos = (LocalContext.current.applicationContext as TikApplication).photos
    val fields = TikTheme.settings.fields
    val formatting = TikTheme.formatting
    val calendar = formatting.calendar
    val colors = TikTheme.colors
    val type = TikTheme.type
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val existing = route.taskId?.let { id -> data.tasks.firstOrNull { it.id == id } }
    if (route.taskId != null && existing == null) {
        // The task was deleted, for example from a reminder.
        LaunchedEffect(Unit) { navigator.close(route) }
        return
    }

    val initial = remember {
        existing?.let { TaskDraft.forTask(it, calendar, Instant.now()) }
            ?: TaskDraft.forNew(calendar, Instant.now(), route.title, route.dueDateMillis?.let(Instant::ofEpochMilli), route.listId)
    }
    val originalText = rememberSaveable { initial.toJson() }
    var draftText by rememberSaveable { mutableStateOf(originalText) }
    val original = remember(originalText) { TaskDraft.fromJson(originalText) }
    var draft by remember { mutableStateOf(TaskDraft.fromJson(draftText)) }
    fun update(change: (TaskDraft) -> TaskDraft) {
        draft = change(draft)
        draftText = draft.toJson()
    }
    val hasChanges = draft != original

    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showRepeat by remember { mutableStateOf(false) }
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    var showWheels by rememberSaveable { mutableStateOf(false) }
    var showPhoto by remember { mutableStateOf(false) }
    var listMenu by remember { mutableStateOf(false) }
    var focusSubtask by remember { mutableStateOf<String?>(null) }
    val titleFocus = remember { FocusRequester() }

    fun close() = navigator.close(route)
    fun cancel() = if (hasChanges) confirmDiscard = true else close()
    fun save() {
        val saved = draft
        model.launch { save(saved, existing) }
        close()
    }
    fun addSubtask(after: String?) {
        val index = draft.subtasks.indexOfFirst { it.id == after }
        if (index >= 0 && draft.subtasks[index].title.isBlank()) {
            // Return on an empty subtask just closes the keyboard.
            focusManager.clearFocus()
            return
        }
        val new = SubtaskDraft(title = "")
        update { it.copy(subtasks = it.subtasks.toMutableList().apply { add(if (index >= 0) index + 1 else size, new) }) }
        focusSubtask = new.id
    }

    BackHandler(enabled = hasChanges) { confirmDiscard = true }
    LaunchedEffect(Unit) {
        if (existing == null && draft.title.isEmpty()) {
            delay(350)
            runCatching { titleFocus.requestFocus() }
        }
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch { photos.import(uri)?.let { name -> update { it.copy(photo = name) } } }
        }
    }

    SheetScaffold(
        title = stringResource(if (existing == null) R.string.new_task else R.string.edit_task),
        onClose = ::cancel,
        onSave = ::save,
        saveEnabled = draft.canSave,
    ) {
        // Title and notes
        Column(Modifier.fillMaxWidth().card().padding(horizontal = 16.dp, vertical = 14.dp)) {
            PlainField(
                value = draft.title,
                onValueChange = { text -> update { it.copy(title = text) } },
                placeholder = stringResource(R.string.what_do_you_want_to_do),
                style = type.title3,
                modifier = Modifier.focusRequester(titleFocus).testTag("titleField"),
                singleLine = true,
                imeAction = ImeAction.Done,
                onImeAction = { focusManager.clearFocus() },
            )
            if (fields.notes) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = colors.separator)
                PlainField(
                    value = draft.note,
                    onValueChange = { text -> update { it.copy(note = text) } },
                    placeholder = stringResource(R.string.notes),
                    style = type.body,
                    minLines = 2,
                    maxLines = 8,
                    modifier = Modifier.testTag("notesField"),
                )
            }
        }

        // When
        if (fields.dates) {
            Spacer(Modifier.height(18.dp))
            val rows = buildList {
                add("date")
                if (draft.hasDate) {
                    add("day")
                    if (showCalendar) add("calendar")
                    add("shortcuts")
                    add("time")
                    if (draft.hasTime) {
                        add("timeValue")
                        if (showWheels) add("wheels")
                    }
                    if (fields.repeat) add("repeat")
                }
            }
            fun position(key: String) = groupPosition(rows.indexOf(key), rows.size)
            Column(Modifier.animateContentSize(Motion.spatial()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                GroupRow(position("date")) {
                    FormLabel(R.drawable.ic_calendar, stringResource(R.string.date))
                    Switch(
                        checked = draft.hasDate,
                        onCheckedChange = { on -> update { it.copy(hasDate = on) } },
                        modifier = Modifier.testTag("dateSwitch"),
                    )
                }
                if (draft.hasDate) {
                    GroupRow(position("day"), onClick = { showCalendar = !showCalendar }) {
                        FormLabel(null, stringResource(R.string.day))
                        ValuePill(dayText(draft.day), active = showCalendar)
                    }
                    if (showCalendar) {
                        GroupRow(position("calendar")) {
                            CalendarPicker(draft.day, onSelect = { day -> update { it.copy(day = day) } })
                        }
                    }
                    GroupRow(position("shortcuts")) {
                        DayShortcuts(draft.day) { day -> update { it.copy(day = day) } }
                    }
                    GroupRow(position("time")) {
                        FormLabel(R.drawable.ic_clock, stringResource(R.string.time))
                        Switch(
                            checked = draft.hasTime,
                            onCheckedChange = { on ->
                                update { it.copy(hasTime = on) }
                                showWheels = on
                            },
                            modifier = Modifier.testTag("timeSwitch"),
                        )
                    }
                    if (draft.hasTime) {
                        GroupRow(position("timeValue"), onClick = { showWheels = !showWheels }) {
                            FormLabel(null, stringResource(R.string.time))
                            ValuePill(formatting.time(calendar.at(draft.day, draft.time.hour, draft.time.minute)), active = showWheels)
                        }
                        if (showWheels) {
                            GroupRow(position("wheels")) {
                                TimeWheels(draft.time, TikTheme.settings.use24Hour, onChange = { time -> update { it.copy(time = time) } })
                            }
                        }
                    }
                    if (fields.repeat) {
                        GroupRow(position("repeat"), onClick = { showRepeat = true }, modifier = Modifier.testTag("repeatRow")) {
                            FormLabel(R.drawable.ic_repeat, stringResource(R.string.repeat))
                            Text(
                                repeatSummary(draft.repeatRule),
                                style = type.body,
                                color = colors.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Icon(painterResource(R.drawable.ic_chevron_end), contentDescription = null, tint = colors.tertiaryText, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // List and priority
        Spacer(Modifier.height(18.dp))
        val organize = if (fields.priority) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box {
                val list = data.list(draft.listId)
                GroupRow(groupPosition(0, organize), onClick = { listMenu = true }, modifier = Modifier.testTag("listRow")) {
                    FormLabel(R.drawable.ic_list_list, stringResource(R.string.list))
                    if (list != null) {
                        IconBadge(list.icon.drawable, list.color.system.color(colors.dark), size = 22.dp, iconSize = 13.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        list?.name ?: stringResource(R.string.inbox),
                        style = type.body,
                        color = colors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(painterResource(R.drawable.ic_expand), contentDescription = null, tint = colors.tertiaryText, modifier = Modifier.size(20.dp))
                }
                GlassMenu(expanded = listMenu, onDismiss = { listMenu = false }) {
                    MenuItem(
                        stringResource(R.string.inbox),
                        if (draft.listId == null) R.drawable.ic_check else R.drawable.ic_inbox,
                    ) {
                        update { it.copy(listId = null) }
                        listMenu = false
                    }
                    data.lists.forEach { item ->
                        MenuItem(
                            item.name,
                            if (draft.listId == item.id) R.drawable.ic_check else item.icon.drawable,
                            tint = item.color.system.color(colors.dark),
                        ) {
                            update { it.copy(listId = item.id) }
                            listMenu = false
                        }
                    }
                }
            }
            if (fields.priority) {
                GroupRow(groupPosition(1, organize)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FormLabel(R.drawable.ic_priority, stringResource(R.string.priority))
                        }
                        SegmentedControl(
                            options = Priority.entries,
                            selected = draft.priority,
                            label = { stringResource(it.title) },
                            onSelect = { priority -> update { it.copy(priority = priority) } },
                            tag = { "priority-${it.name.lowercase()}" },
                        )
                    }
                }
            }
        }

        // Subtasks
        if (fields.subtasks) {
            SectionTitle(stringResource(R.string.subtasks))
            Column(Modifier.fillMaxWidth().card().padding(vertical = 4.dp).animateContentSize(Motion.spatial())) {
                ReorderableColumn(
                    list = draft.subtasks,
                    onSettle = { from, to ->
                        update { it.copy(subtasks = it.subtasks.toMutableList().apply { add(to, removeAt(from)) }) }
                    },
                ) { _, subtask, _ ->
                    key(subtask.id) {
                        ReorderableItem {
                            SubtaskRow(
                                subtask = subtask,
                                focus = focusSubtask == subtask.id,
                                onFocused = { focusSubtask = null },
                                onChange = { changed -> update { d -> d.copy(subtasks = d.subtasks.map { if (it.id == changed.id) changed else it }) } },
                                onRemove = { update { d -> d.copy(subtasks = d.subtasks.filterNot { it.id == subtask.id }) } },
                                onNext = { addSubtask(after = subtask.id) },
                                handle = Modifier.draggableHandle(),
                            )
                        }
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) { addSubtask(null) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("addSubtask"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.add_subtask), style = type.body, color = colors.accent)
                }
            }
        }

        // Photo
        if (fields.photos) {
            Spacer(Modifier.height(18.dp))
            val photo = draft.photo
            val photoRows = if (photo != null) 3 else 1
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (photo != null) {
                    val description = stringResource(R.string.photo)
                    AsyncImage(
                        model = photos.file(photo),
                        contentDescription = description,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                            .clickable { showPhoto = true },
                    )
                }
                GroupRow(
                    groupPosition(if (photo != null) 1 else 0, photoRows),
                    onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                ) {
                    Icon(painterResource(R.drawable.ic_add_photo), contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(if (photo == null) R.string.add_photo else R.string.change_photo), style = type.body, color = colors.accent)
                }
                if (photo != null) {
                    GroupRow(groupPosition(2, photoRows), onClick = { update { it.copy(photo = null) } }) {
                        Icon(painterResource(R.drawable.ic_delete), contentDescription = null, tint = colors.destructive, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.remove_photo), style = type.body, color = colors.destructive)
                    }
                }
            }
        }

        // Delete
        if (existing != null) {
            Spacer(Modifier.height(28.dp))
            GroupRow(groupPosition(0, 1), onClick = { confirmDelete = true }, modifier = Modifier.testTag("deleteTask")) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = null, tint = colors.destructive, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.delete_task), style = type.body, color = colors.destructive)
            }
        }
    }

    if (showRepeat) {
        RepeatSheet(
            rule = draft.repeatRule,
            day = draft.day,
            calendar = calendar,
            onChange = { rule -> update { it.copy(repeatRule = rule) } },
            onDismiss = { showRepeat = false },
        )
    }
    if (showPhoto) {
        draft.photo?.let { PhotoViewer(photos.file(it), onDismiss = { showPhoto = false }) }
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_your_changes)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    close()
                }) { Text(stringResource(R.string.discard_changes), color = colors.destructive) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.keep_editing)) }
            },
            containerColor = colors.glassFallback,
        )
    }
    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_this_task)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    close()
                    model.launch { delete(existing) }
                }) { Text(stringResource(R.string.delete_task), color = colors.destructive) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
            containerColor = colors.glassFallback,
        )
    }
}

/** "Thu, Mehr 2", with the year when it isn't this year. */
@Composable
private fun dayText(day: LocalDate): String {
    val formatting = TikTheme.formatting
    val instant = formatting.calendar.startOfDay(day)
    return if (formatting.calendar.isSameYear(instant, Instant.now())) formatting.shortDay(instant) else formatting.dayMonthYear(instant)
}

/** An icon (in the accent color) and a label, taking the free space in a form row. */
@Composable
private fun RowScope.FormLabel(icon: Int?, text: String) {
    val colors = TikTheme.colors
    if (icon != null) {
        Icon(painterResource(icon), contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
    }
    Text(text, style = TikTheme.type.body, color = colors.primaryText, modifier = Modifier.weight(1f))
}

/** The current value of a date or time row. It turns accent-colored while its picker is open. */
@Composable
private fun ValuePill(text: String, active: Boolean) {
    val colors = TikTheme.colors
    Text(
        text,
        style = TikTheme.type.body.copy(fontWeight = FontWeight.Medium),
        color = if (active) colors.accent else colors.primaryText,
        maxLines = 1,
        modifier = Modifier
            .background(colors.fill, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** Today, Tomorrow and Next Week, one tap each. */
@Composable
private fun DayShortcuts(selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val colors = TikTheme.colors
    val feedback = rememberFeedback()
    val calendar = TikTheme.formatting.calendar
    val today = remember(calendar) { calendar.localDate(Instant.now()) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(R.string.today to 0L, R.string.tomorrow to 1L, R.string.next_week to 7L).forEach { (label, days) ->
            val day = today.plusDays(days)
            val isSelected = day == selected
            Box(
                Modifier
                    .weight(1f)
                    .background(if (isSelected) colors.accent else colors.fill, CapsuleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                        feedback.selection()
                        onSelect(day)
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(label),
                    style = TikTheme.type.subheadline.copy(fontWeight = FontWeight.Medium),
                    color = if (isSelected) colors.onAccent else colors.primaryText,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SubtaskRow(
    subtask: SubtaskDraft,
    focus: Boolean,
    onFocused: () -> Unit,
    onChange: (SubtaskDraft) -> Unit,
    onRemove: () -> Unit,
    onNext: () -> Unit,
    handle: Modifier,
) {
    val colors = TikTheme.colors
    val feedback = rememberFeedback()
    val requester = remember { FocusRequester() }
    LaunchedEffect(focus) {
        if (focus) {
            delay(60)
            runCatching { requester.requestFocus() }
            onFocused()
        }
    }
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        val label = stringResource(if (subtask.isDone) R.string.mark_as_not_done else R.string.mark_as_done)
        Box(
            Modifier
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Checkbox) {
                    feedback.tick(!subtask.isDone)
                    onChange(subtask.copy(isDone = !subtask.isDone))
                }
                .semantics { contentDescription = label }
                .padding(vertical = 8.dp),
        ) {
            CheckCircle(isDone = subtask.isDone, size = 20.dp)
        }
        Spacer(Modifier.width(12.dp))
        PlainField(
            value = subtask.title,
            onValueChange = { onChange(subtask.copy(title = it)) },
            placeholder = stringResource(R.string.subtask),
            style = TikTheme.type.body.copy(textDecoration = if (subtask.isDone) TextDecoration.LineThrough else null),
            color = if (subtask.isDone) colors.secondaryText else colors.primaryText,
            singleLine = true,
            imeAction = ImeAction.Next,
            onImeAction = onNext,
            modifier = Modifier.weight(1f).focusRequester(requester),
        )
        val remove = stringResource(R.string.delete)
        Box(
            Modifier
                .size(40.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onRemove)
                .semantics { contentDescription = remove },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_close), contentDescription = null, tint = colors.tertiaryText, modifier = Modifier.size(18.dp))
        }
        val reorder = stringResource(R.string.reorder)
        Icon(
            painterResource(R.drawable.ic_drag),
            contentDescription = reorder,
            tint = colors.tertiaryText,
            modifier = handle.size(40.dp).padding(8.dp),
        )
    }
}

/** A text field with no box around it, like the fields in an iOS form. */
@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = TikTheme.colors.primaryText,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: () -> Unit = {},
) {
    val colors = TikTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = style.copy(color = color),
        cursorBrush = SolidColor(colors.accent),
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        decorationBox = { field ->
            Box {
                if (value.isEmpty()) Text(placeholder, style = style, color = colors.tertiaryText)
                field()
            }
        },
    )
}
