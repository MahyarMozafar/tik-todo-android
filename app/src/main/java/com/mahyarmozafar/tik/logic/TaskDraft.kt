package com.mahyarmozafar.tik.logic

import com.mahyarmozafar.tik.model.Priority
import com.mahyarmozafar.tik.model.RepeatRule
import com.mahyarmozafar.tik.model.Subtask
import com.mahyarmozafar.tik.model.Task
import com.mahyarmozafar.tik.model.newId
import com.mahyarmozafar.tik.time.TikCalendar
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class SubtaskDraft(
    val id: String = newId(),
    val title: String,
    val isDone: Boolean = false,
)

/**
 * A copy of a task's values that the editor can change freely.
 * Nothing is saved until [applyTo] is used.
 */
data class TaskDraft(
    val title: String = "",
    val note: String = "",
    val listId: String? = null,
    val priority: Priority = Priority.None,
    val hasDate: Boolean = false,
    val day: LocalDate,
    val hasTime: Boolean = false,
    val time: LocalTime,
    val repeatRule: RepeatRule? = null,
    val subtasks: List<SubtaskDraft> = emptyList(),
    val photo: String? = null,
) {
    val trimmedTitle: String get() = title.trim()

    val canSave: Boolean get() = trimmedTitle.isNotEmpty()

    /** The due date made from [day] and [time], or null when there is no date. */
    fun dueDate(calendar: TikCalendar): Instant? = when {
        !hasDate -> null
        !hasTime -> calendar.startOfDay(day)
        else -> calendar.at(day, time.hour, time.minute)
    }

    /** Writes the draft into [task] (or a new task), including its subtasks. */
    fun applyTo(task: Task?, calendar: TikCalendar, now: Instant): Task {
        val due = dueDate(calendar)
        val rule = if (hasDate) {
            repeatRule?.let { rule ->
                // Remember the day of the month, so monthly repeats stay on it.
                val keepsDay = rule.frequency == RepeatRule.Frequency.Monthly ||
                    rule.frequency == RepeatRule.Frequency.Yearly
                if (due != null && keepsDay) rule.copy(dayOfMonth = calendar.dayOfMonth(due)) else rule
            }
        } else {
            null
        }
        val cleanSubtasks = subtasks.mapIndexedNotNull { index, draft ->
            val title = draft.title.trim()
            if (title.isEmpty()) null else Subtask(id = draft.id, title = title, isDone = draft.isDone, sortIndex = index)
        }
        val base = task ?: Task(title = trimmedTitle, createdAt = now)
        return base.copy(
            title = trimmedTitle,
            note = note.trim(),
            listId = listId,
            priority = priority,
            dueDate = due,
            hasTime = hasDate && hasTime,
            repeatRule = rule,
            photo = photo,
            subtasks = cleanSubtasks,
        )
    }

    companion object {
        fun forTask(task: Task, calendar: TikCalendar, now: Instant): TaskDraft {
            val due = task.dueDate
            return TaskDraft(
                title = task.title,
                note = task.note,
                listId = task.listId,
                priority = task.priority,
                hasDate = due != null,
                day = calendar.localDate(due ?: now),
                hasTime = due != null && task.hasTime,
                time = if (due != null && task.hasTime) calendar.localTime(due) else nextFullHour(now, calendar),
                repeatRule = task.repeatRule,
                subtasks = task.subtasks.map { SubtaskDraft(id = it.id, title = it.title, isDone = it.isDone) },
                photo = task.photo,
            )
        }

        fun forNew(
            calendar: TikCalendar,
            now: Instant,
            title: String = "",
            dueDate: Instant? = null,
            listId: String? = null,
        ): TaskDraft = TaskDraft(
            title = title,
            listId = listId,
            hasDate = dueDate != null,
            day = calendar.localDate(dueDate ?: now),
            time = nextFullHour(now, calendar),
        )

        /** The next full hour from now, a good guess for a new task's time. */
        fun nextFullHour(now: Instant, calendar: TikCalendar): LocalTime =
            calendar.localTime(now).plusHours(1).withMinute(0).withSecond(0).withNano(0)
    }
}
