package com.mahyarmozafar.tik.data

import androidx.room.withTransaction
import com.mahyarmozafar.tik.model.Task
import com.mahyarmozafar.tik.model.TaskList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Reads and writes tasks and lists. Screens watch [tasks] and [lists]; changes go through here. */
class TikStore(private val database: TikDatabase, private val photos: PhotoStore) {
    private val dao = database.dao()

    val tasks: Flow<List<Task>> = dao.observeTasks().map { rows -> rows.map { it.toTask() } }

    val lists: Flow<List<TaskList>> = dao.observeLists().map { rows -> rows.map { it.toList() } }

    suspend fun allTasks(): List<Task> = dao.tasks().map { it.toTask() }

    suspend fun task(id: String): Task? = dao.task(id)?.toTask()

    suspend fun allLists(): List<TaskList> = dao.lists().map { it.toList() }

    suspend fun listCount(): Int = dao.listCount()

    /** Saves tasks with their subtasks. Subtasks that are no longer on a task are removed. */
    suspend fun save(tasks: List<Task>) {
        if (tasks.isEmpty()) return
        database.withTransaction {
            dao.upsertTasks(tasks.map { it.toEntity() })
            for (task in tasks) {
                dao.deleteSubtasksExcept(task.id, task.subtasks.map { it.id })
                dao.upsertSubtasks(task.subtaskEntities())
            }
        }
    }

    suspend fun save(vararg tasks: Task) = save(tasks.toList())

    suspend fun delete(ids: List<String>) {
        if (ids.isEmpty()) return
        dao.deleteTasks(ids)
        removeUnusedPhotos()
    }

    suspend fun saveLists(lists: List<TaskList>) = dao.upsertLists(lists.map { it.toEntity() })

    /** Deletes a list and every task in it. Returns the ids of those tasks. */
    suspend fun deleteList(id: String): List<String> {
        val taskIds = dao.taskIdsInList(id)
        dao.deleteList(id)
        removeUnusedPhotos()
        return taskIds
    }

    /** Throws everything away and saves these instead. Used for the demo data. */
    suspend fun replaceAll(lists: List<TaskList>, tasks: List<Task>) {
        database.withTransaction {
            dao.deleteAllTasks()
            dao.deleteAllLists()
            dao.upsertLists(lists.map { it.toEntity() })
            dao.upsertTasks(tasks.map { it.toEntity() })
            dao.upsertSubtasks(tasks.flatMap { it.subtaskEntities() })
        }
        removeUnusedPhotos()
    }

    /** A photo can be shared by the copies of a repeating task, so it is removed only when no task uses it. */
    suspend fun removeUnusedPhotos() {
        photos.deleteAllExcept(dao.photosInUse().toSet())
    }
}
