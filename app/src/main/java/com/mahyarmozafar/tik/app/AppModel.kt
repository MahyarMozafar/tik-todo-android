package com.mahyarmozafar.tik.app

import android.content.Context
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.data.SettingsRepository
import com.mahyarmozafar.tik.data.TikStore
import com.mahyarmozafar.tik.logic.TaskActions
import com.mahyarmozafar.tik.logic.TaskDraft
import com.mahyarmozafar.tik.logic.TaskFilter
import com.mahyarmozafar.tik.model.AccentChoice
import com.mahyarmozafar.tik.model.ListIcon
import com.mahyarmozafar.tik.model.Priority
import com.mahyarmozafar.tik.model.Task
import com.mahyarmozafar.tik.model.TaskList
import com.mahyarmozafar.tik.model.TikSettings
import com.mahyarmozafar.tik.reminders.DayChange
import com.mahyarmozafar.tik.reminders.Reminders
import com.mahyarmozafar.tik.reminders.TodayCount
import com.mahyarmozafar.tik.widget.TodayWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

enum class AppTab { Today, Lists, Search }

/** Everything the screens show, loaded together so nothing flashes empty at launch. */
data class AppData(val tasks: List<Task>, val lists: List<TaskList>) {
    private val listsById = lists.associateBy { it.id }

    fun list(id: String?): TaskList? = id?.let(listsById::get)
}

/**
 * App-wide state, and the actions that change tasks.
 *
 * Every change goes through here, so saving, reminders, the widget and the count on the icon
 * stay in step no matter which screen (or widget, or reminder button) made the change.
 */
class AppModel(
    private val context: Context,
    private val store: TikStore,
    private val settingsRepository: SettingsRepository,
    private val reminders: Reminders,
    private val todayCount: TodayCount,
    private val scope: CoroutineScope,
) {
    val settings: StateFlow<TikSettings?> =
        settingsRepository.settings.stateIn(scope, SharingStarted.Eagerly, null)

    val data: StateFlow<AppData?> =
        combine(store.tasks, store.lists) { tasks, lists -> AppData(tasks, lists) }
            .stateIn(scope, SharingStarted.Eagerly, null)

    val selectedTab = MutableStateFlow(AppTab.Today)

    /** Set when the widget's + button opens the app. Today then opens its quick add field. */
    val pendingQuickAdd = MutableStateFlow(false)

    /** A screen to open at launch, for screenshots in debug builds (see [DemoData]). */
    val startScreen = MutableStateFlow<String?>(null)

    private val _celebrations = MutableStateFlow(0)

    /** Goes up by one each time the last open task of today is ticked. */
    val celebrations: StateFlow<Int> = _celebrations.asStateFlow()

    private val _permissionRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Asks the screen to ask for notification permission (the first task with a time does). */
    val permissionRequests: SharedFlow<Unit> = _permissionRequests.asSharedFlow()

    /** Changes are made one at a time, so two quick ticks never read the same old state. */
    private val mutex = Mutex()

    fun launch(block: suspend AppModel.() -> Unit): Job = scope.launch { block() }

    // Tasks

    suspend fun addTask(title: String, dueDate: Instant?, listId: String? = null): Task = change {
        val task = Task(title = title.trim(), dueDate = dueDate, listId = listId)
        store.save(task)
        task
    }

    /** Saves what the editor changed. With no [existing] task, a new one is made. */
    suspend fun save(draft: TaskDraft, existing: Task?): Task = change {
        val settings = settingsRepository.current()
        val task = draft.applyTo(existing, settings.formatting().calendar, Instant.now())
        store.save(task)
        if (existing?.photo != null && existing.photo != task.photo) store.removeUnusedPhotos()
        reminders.sync(task, settings)
        if (task.hasTime && !task.isDone && !reminders.canNotify) _permissionRequests.tryEmit(Unit)
        task
    }

    /** Ticks or unticks a task. Returns true when it is now done, or null when it is gone. */
    suspend fun toggle(taskId: String): Boolean? = change {
        val task = store.task(taskId) ?: return@change null
        val settings = settingsRepository.current()
        val calendar = settings.formatting().calendar
        val now = Instant.now()

        val result = TaskActions.toggle(task, store.allTasks(), calendar, now)
        store.save(listOfNotNull(result.task, result.nextOccurrence))
        store.delete(result.removedIds)

        reminders.sync(result.task, settings)
        result.nextOccurrence?.let { reminders.sync(it, settings) }
        reminders.cancel(result.removedIds)

        if (result.isDone && settings.celebration && TaskFilter.isOnToday(result.task, now, calendar)) {
            val openToday = TaskFilter.today(store.allTasks(), now, calendar, settings.sortOrder).count { !it.isDone }
            if (openToday == 0) _celebrations.value += 1
        }
        result.isDone
    }

    suspend fun delete(task: Task) = change {
        store.delete(listOf(task.id))
        reminders.cancel(listOf(task.id))
    }

    suspend fun moveToTomorrow(task: Task) = change {
        val settings = settingsRepository.current()
        val moved = TaskActions.moveToTomorrow(task, settings.formatting().calendar, Instant.now())
        store.save(moved)
        reminders.sync(moved, settings)
    }

    suspend fun duplicate(task: Task) = change {
        val copy = TaskActions.duplicate(task, Instant.now())
        store.save(copy)
        reminders.sync(copy, settingsRepository.current())
    }

    suspend fun setPriority(task: Task, priority: Priority) = change {
        store.task(task.id)?.let { store.save(it.copy(priority = priority)) }
    }

    /** The "Mark as Done" button on a reminder. */
    suspend fun markDoneFromReminder(taskId: String) {
        val task = store.task(taskId) ?: return
        if (!task.isDone) toggle(taskId)
    }

    // Lists

    suspend fun saveList(existing: TaskList?, name: String, icon: ListIcon, color: AccentChoice) = change {
        val list = existing?.copy(name = name.trim(), icon = icon, color = color)
            ?: TaskList(name = name.trim(), icon = icon, color = color, sortIndex = store.listCount())
        store.saveLists(listOf(list))
    }

    /** Deletes a list and every task in it. */
    suspend fun deleteList(list: TaskList) = change {
        reminders.cancel(store.deleteList(list.id))
    }

    suspend fun reorderLists(ordered: List<TaskList>) = change {
        store.saveLists(ordered.mapIndexed { index, list -> list.copy(sortIndex = index) })
    }

    // Settings

    /** Changes settings and brings reminders and the widget up to date. */
    suspend fun updateSettings(transform: (TikSettings) -> TikSettings) = change {
        val old = settingsRepository.current()
        settingsRepository.update(transform)
        val new = settingsRepository.current()
        if (old.language != new.language) reminders.createChannels(new.language)
        if (old.language != new.language || old.use24Hour != new.use24Hour ||
            old.reminderOffsetMinutes != new.reminderOffsetMinutes
        ) {
            reminders.resyncAll(store.allTasks(), new)
        }
    }

    /** Schedules every reminder again, for example after the phone restarts. */
    suspend fun refreshAll() {
        val settings = settingsRepository.current()
        reminders.createChannels(settings.language)
        reminders.resyncAll(store.allTasks(), settings)
        DayChange.schedule(context)
        afterChange()
    }

    suspend fun start() {
        createStarterListsIfNeeded()
        store.removeUnusedPhotos()
        refreshAll()
    }

    // Demo data (debug builds only)

    suspend fun replaceAll(lists: List<TaskList>, tasks: List<Task>) = change {
        store.replaceAll(lists, tasks)
        reminders.resyncAll(tasks, settingsRepository.current())
    }

    suspend fun resetSettings() = settingsRepository.reset()

    suspend fun currentSettings(): TikSettings = settingsRepository.current()

    // Helpers

    private suspend fun <T> change(block: suspend () -> T): T {
        val result = mutex.withLock { block() }
        afterChange()
        return result
    }

    /** The widget and the count on the icon follow every change. Nobody waits for the widget. */
    private suspend fun afterChange() {
        scope.launch { TodayWidget.updateAll(context) }
        val settings = settingsRepository.current()
        val calendar = settings.formatting().calendar
        val openToday = TaskFilter.today(store.allTasks(), Instant.now(), calendar, settings.sortOrder).count { !it.isDone }
        todayCount.update(openToday, settings)
    }

    private suspend fun createStarterListsIfNeeded() {
        if (!settingsRepository.claimStarterLists() || store.listCount() > 0) return
        val texts = context.localized(settingsRepository.current().language)
        store.saveLists(
            listOf(
                TaskList(name = texts.getString(R.string.personal), icon = ListIcon.House, color = AccentChoice.Blue, sortIndex = 0),
                TaskList(name = texts.getString(R.string.work), icon = ListIcon.Briefcase, color = AccentChoice.Orange, sortIndex = 1),
            ),
        )
    }
}
