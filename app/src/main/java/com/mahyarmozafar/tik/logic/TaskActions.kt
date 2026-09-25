package com.mahyarmozafar.tik.logic

import com.mahyarmozafar.tik.model.RepeatRule
import com.mahyarmozafar.tik.model.Subtask
import com.mahyarmozafar.tik.model.Task
import com.mahyarmozafar.tik.time.TikCalendar
import java.time.Instant

/**
 * Changes to tasks that the app, the widget and the reminder buttons all share.
 * These functions only work out the new values; saving them is up to the caller.
 */
object TaskActions {

    data class ToggleResult(
        /** The task after the tick or untick. */
        val task: Task,
        /** The next copy of a repeating task, made when it was ticked. */
        val nextOccurrence: Task? = null,
        /** Copies that go away because a tick was undone. */
        val removedIds: List<String> = emptyList(),
    ) {
        val isDone: Boolean get() = task.isDone
    }

    /**
     * Ticks or unticks a task. Ticking a repeating task also makes its next copy. Unticking it
     * removes that copy again, as long as the copy itself was not ticked yet.
     */
    fun toggle(task: Task, allTasks: List<Task>, calendar: TikCalendar, now: Instant): ToggleResult {
        if (task.isDone) {
            val copies = allTasks.filter { it.previousOccurrenceId == task.id && !it.isDone }.map { it.id }
            return ToggleResult(task.copy(isDone = false, completedAt = null), removedIds = copies)
        }
        val next = task.repeatRule?.let { makeNextOccurrence(task, it, calendar, now) }
        return ToggleResult(task.copy(isDone = true, completedAt = now), nextOccurrence = next)
    }

    fun makeNextOccurrence(task: Task, rule: RepeatRule, calendar: TikCalendar, now: Instant): Task {
        val due = task.dueDate ?: calendar.startOfDay(now)
        return Task(
            title = task.title,
            note = task.note,
            createdAt = now,
            dueDate = rule.nextDueDate(due, now, calendar),
            hasTime = task.hasTime,
            priority = task.priority,
            repeatRule = task.repeatRule,
            photo = task.photo,
            previousOccurrenceId = task.id,
            listId = task.listId,
            subtasks = freshSubtasks(task),
        )
    }

    /** Moves a task to tomorrow, keeping its time of day. */
    fun moveToTomorrow(task: Task, calendar: TikCalendar, now: Instant): Task {
        val tomorrow = calendar.addDays(calendar.startOfDay(now), 1)
        val due = task.dueDate
        if (task.hasTime && due != null) {
            val time = calendar.localTime(due)
            return task.copy(dueDate = calendar.at(calendar.localDate(tomorrow), time.hour, time.minute))
        }
        return task.copy(dueDate = tomorrow, hasTime = false)
    }

    /** A fresh, not-done copy of a task. */
    fun duplicate(task: Task, now: Instant): Task = Task(
        title = task.title,
        note = task.note,
        createdAt = now,
        dueDate = task.dueDate,
        hasTime = task.hasTime,
        priority = task.priority,
        repeatRule = task.repeatRule,
        photo = task.photo,
        listId = task.listId,
        subtasks = freshSubtasks(task),
    )

    private fun freshSubtasks(task: Task): List<Subtask> =
        task.subtasks.sortedBy { it.sortIndex }.map { Subtask(title = it.title, sortIndex = it.sortIndex) }
}
