package com.mahyarmozafar.tik.model

import java.time.Instant
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

data class Task(
    val id: String = newId(),
    val title: String,
    val note: String = "",
    val isDone: Boolean = false,
    val completedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    /** The day the task is for. When [hasTime] is false this is the start of that day. */
    val dueDate: Instant? = null,
    val hasTime: Boolean = false,
    val priority: Priority = Priority.None,
    val repeatRule: RepeatRule? = null,
    /** The file name of the task's photo, inside the app's photo folder. */
    val photo: String? = null,
    /** Set on the copy made when a repeating task is ticked, so the copy can go again on untick. */
    val previousOccurrenceId: String? = null,
    /** Null means the task is in the Inbox. */
    val listId: String? = null,
    /** Always sorted by [Subtask.sortIndex]. */
    val subtasks: List<Subtask> = emptyList(),
) {
    val hasPhoto: Boolean get() = photo != null
    val doneSubtaskCount: Int get() = subtasks.count { it.isDone }
}

data class Subtask(
    val id: String = newId(),
    val title: String,
    val isDone: Boolean = false,
    val sortIndex: Int = 0,
)

data class TaskList(
    val id: String = newId(),
    val name: String,
    val icon: ListIcon = ListIcon.List,
    val color: AccentChoice = AccentChoice.Blue,
    val sortIndex: Int = 0,
    val createdAt: Instant = Instant.now(),
)
