package com.mahyarmozafar.tik.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.mahyarmozafar.tik.model.AccentChoice
import com.mahyarmozafar.tik.model.ListIcon
import com.mahyarmozafar.tik.model.Priority
import com.mahyarmozafar.tik.model.RepeatRule
import com.mahyarmozafar.tik.model.Subtask
import com.mahyarmozafar.tik.model.Task
import com.mahyarmozafar.tik.model.TaskList
import java.time.Instant

@Entity(tableName = "lists")
data class ListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val sortIndex: Int,
    val createdAt: Long,
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("listId"), Index("previousOccurrenceId"), Index("dueDate")],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val note: String,
    val isDone: Boolean,
    val completedAt: Long?,
    val createdAt: Long,
    val dueDate: Long?,
    val hasTime: Boolean,
    val priority: Int,
    /** A [RepeatRule] as JSON, or null when the task does not repeat. */
    val repeatRule: String?,
    val photo: String?,
    val previousOccurrenceId: String?,
    val listId: String?,
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId")],
)
data class SubtaskEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val title: String,
    val isDone: Boolean,
    val sortIndex: Int,
)

data class TaskWithSubtasks(
    @Embedded val task: TaskEntity,
    @Relation(parentColumn = "id", entityColumn = "taskId")
    val subtasks: List<SubtaskEntity>,
)

fun TaskWithSubtasks.toTask(): Task = Task(
    id = task.id,
    title = task.title,
    note = task.note,
    isDone = task.isDone,
    completedAt = task.completedAt?.let(Instant::ofEpochMilli),
    createdAt = Instant.ofEpochMilli(task.createdAt),
    dueDate = task.dueDate?.let(Instant::ofEpochMilli),
    hasTime = task.hasTime,
    priority = Priority.fromRaw(task.priority),
    repeatRule = RepeatRule.fromJson(task.repeatRule),
    photo = task.photo,
    previousOccurrenceId = task.previousOccurrenceId,
    listId = task.listId,
    subtasks = subtasks.sortedBy { it.sortIndex }.map { Subtask(it.id, it.title, it.isDone, it.sortIndex) },
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    note = note,
    isDone = isDone,
    completedAt = completedAt?.toEpochMilli(),
    createdAt = createdAt.toEpochMilli(),
    dueDate = dueDate?.toEpochMilli(),
    hasTime = hasTime,
    priority = priority.raw,
    repeatRule = repeatRule?.toJson(),
    photo = photo,
    previousOccurrenceId = previousOccurrenceId,
    listId = listId,
)

fun Task.subtaskEntities(): List<SubtaskEntity> =
    subtasks.map { SubtaskEntity(it.id, id, it.title, it.isDone, it.sortIndex) }

fun ListEntity.toList(): TaskList = TaskList(
    id = id,
    name = name,
    icon = ListIcon.fromKey(icon),
    color = AccentChoice.fromKey(color) ?: AccentChoice.Blue,
    sortIndex = sortIndex,
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun TaskList.toEntity(): ListEntity = ListEntity(id, name, icon.key, color.key, sortIndex, createdAt.toEpochMilli())
