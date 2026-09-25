package com.mahyarmozafar.tik.logic

import com.mahyarmozafar.tik.model.Task
import com.mahyarmozafar.tik.model.TaskSortOrder
import com.mahyarmozafar.tik.time.TikCalendar
import java.text.Normalizer
import java.time.Instant
import java.util.Locale

/** Tasks that share a day, like a section in Scheduled. */
data class DayGroup(val day: Instant, val tasks: List<Task>)

/**
 * Decides which tasks show up on each screen, and in what order.
 * Everything here is a plain function, so it is easy to test.
 */
object TaskFilter {

    // Today

    /**
     * A task is on Today when it is due today or late. A done task stays on Today until the day
     * ends, unless [includeCompleted] is false.
     */
    fun isOnToday(task: Task, now: Instant, calendar: TikCalendar, includeCompleted: Boolean = true): Boolean {
        val due = task.dueDate ?: return false
        val startOfToday = calendar.startOfDay(now)
        val startOfTomorrow = calendar.addDays(startOfToday, 1)

        if (task.isDone) {
            val completedAt = task.completedAt
            if (!includeCompleted || completedAt == null) return false
            return completedAt >= startOfToday && due < startOfTomorrow
        }
        return due < startOfTomorrow
    }

    fun today(
        tasks: List<Task>,
        now: Instant,
        calendar: TikCalendar,
        order: TaskSortOrder,
        includeCompleted: Boolean = true,
    ): List<Task> = sorted(tasks.filter { isOnToday(it, now, calendar, includeCompleted) }, order, now, calendar)

    fun isOverdue(task: Task, now: Instant, calendar: TikCalendar): Boolean {
        val due = task.dueDate ?: return false
        return !task.isDone && due < calendar.startOfDay(now)
    }

    /** Late, or due at a time that has already passed. Late tasks show their date in red. */
    fun isLate(task: Task, now: Instant, calendar: TikCalendar): Boolean {
        val due = task.dueDate ?: return false
        return !task.isDone && (isOverdue(task, now, calendar) || (task.hasTime && due < now))
    }

    // Other screens

    /** Open tasks with a date after today, grouped by day. */
    fun scheduled(tasks: List<Task>, now: Instant, calendar: TikCalendar, order: TaskSortOrder): List<DayGroup> {
        val startOfTomorrow = calendar.addDays(calendar.startOfDay(now), 1)
        val upcoming = tasks.filter { task ->
            val due = task.dueDate
            !task.isDone && due != null && due >= startOfTomorrow
        }
        return upcoming
            .groupBy { calendar.startOfDay(it.dueDate!!) }
            .toSortedMap()
            .map { (day, dayTasks) -> DayGroup(day, sorted(dayTasks, order, now, calendar)) }
    }

    /** Done tasks grouped by the day they were done, newest day first. */
    fun completed(tasks: List<Task>, calendar: TikCalendar): List<DayGroup> =
        tasks.filter { it.isDone }
            .groupBy { calendar.startOfDay(it.completedAt ?: it.createdAt) }
            .toSortedMap(reverseOrder())
            .map { (day, dayTasks) -> DayGroup(day, dayTasks.sortedByDescending { it.completedAt ?: Instant.MIN }) }

    /** Matches the title, the note, or any subtask. Case, accents and Arabic letter forms are ignored. */
    fun matches(task: Task, query: String): Boolean {
        val needle = fold(query.trim())
        if (needle.isEmpty()) return false
        return fold(task.title).contains(needle) ||
            fold(task.note).contains(needle) ||
            task.subtasks.any { fold(it.title).contains(needle) }
    }

    private val marks = Regex("\\p{Mn}+")

    private fun fold(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(marks, "")
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .lowercase(Locale.ROOT)

    // Sorting

    /** Open tasks first, in the chosen order. Done tasks go to the bottom, the most recently done first. */
    fun sorted(tasks: List<Task>, order: TaskSortOrder, now: Instant, calendar: TikCalendar): List<Task> {
        val startOfToday = calendar.startOfDay(now)
        val comparator = Comparator<Task> { a, b ->
            when {
                comesBefore(a, b, order, startOfToday) -> -1
                comesBefore(b, a, order, startOfToday) -> 1
                else -> 0
            }
        }
        val open = tasks.filter { !it.isDone }.sortedWith(comparator)
        val done = tasks.filter { it.isDone }.sortedByDescending { it.completedAt ?: Instant.MIN }
        return open + done
    }

    fun comesBefore(a: Task, b: Task, order: TaskSortOrder, startOfToday: Instant): Boolean {
        if (order == TaskSortOrder.Newest) return a.createdAt > b.createdAt

        // Late tasks always come first.
        val aLate = (a.dueDate ?: Instant.MAX) < startOfToday
        val bLate = (b.dueDate ?: Instant.MAX) < startOfToday
        if (aLate != bLate) return aLate

        if (order == TaskSortOrder.Priority && a.priority != b.priority) {
            return a.priority.raw > b.priority.raw
        }

        // Tasks with a time come before tasks without one, earliest first.
        val aTime = if (a.hasTime) a.dueDate else null
        val bTime = if (b.hasTime) b.dueDate else null
        when {
            aTime != null && bTime != null && aTime != bTime -> return aTime < bTime
            aTime != null && bTime == null -> return true
            aTime == null && bTime != null -> return false
        }

        if (a.priority != b.priority) return a.priority.raw > b.priority.raw
        return a.createdAt < b.createdAt
    }
}
