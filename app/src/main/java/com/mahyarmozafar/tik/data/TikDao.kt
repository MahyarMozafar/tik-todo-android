package com.mahyarmozafar.tik.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TikDao {
    @Transaction
    @Query("SELECT * FROM tasks")
    fun observeTasks(): Flow<List<TaskWithSubtasks>>

    @Query("SELECT * FROM lists ORDER BY sortIndex, createdAt")
    fun observeLists(): Flow<List<ListEntity>>

    @Transaction
    @Query("SELECT * FROM tasks")
    suspend fun tasks(): List<TaskWithSubtasks>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun task(id: String): TaskWithSubtasks?

    @Query("SELECT * FROM lists ORDER BY sortIndex, createdAt")
    suspend fun lists(): List<ListEntity>

    @Query("SELECT COUNT(*) FROM lists")
    suspend fun listCount(): Int

    @Upsert
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Upsert
    suspend fun upsertSubtasks(subtasks: List<SubtaskEntity>)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId AND id NOT IN (:keep)")
    suspend fun deleteSubtasksExcept(taskId: String, keep: List<String>)

    @Query("DELETE FROM tasks WHERE id IN (:ids)")
    suspend fun deleteTasks(ids: List<String>)

    @Upsert
    suspend fun upsertLists(lists: List<ListEntity>)

    @Query("DELETE FROM lists WHERE id = :id")
    suspend fun deleteList(id: String)

    @Query("SELECT photo FROM tasks WHERE photo IS NOT NULL")
    suspend fun photosInUse(): List<String>

    @Query("SELECT photo FROM tasks WHERE id IN (:ids) AND photo IS NOT NULL")
    suspend fun photosOf(ids: List<String>): List<String>

    @Query("SELECT id FROM tasks WHERE listId = :listId")
    suspend fun taskIdsInList(listId: String): List<String>

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("DELETE FROM lists")
    suspend fun deleteAllLists()
}
