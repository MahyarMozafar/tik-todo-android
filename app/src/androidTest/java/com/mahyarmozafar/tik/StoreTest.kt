package com.mahyarmozafar.tik

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.mahyarmozafar.tik.data.PhotoStore
import com.mahyarmozafar.tik.data.TikDatabase
import com.mahyarmozafar.tik.data.TikStore
import com.mahyarmozafar.tik.model.Priority
import com.mahyarmozafar.tik.model.RepeatRule
import com.mahyarmozafar.tik.model.Subtask
import com.mahyarmozafar.tik.model.Task
import com.mahyarmozafar.tik.model.TaskList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class StoreTest {
    private lateinit var database: TikDatabase
    private lateinit var store: TikStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = TikDatabase.inMemory(context)
        store = TikStore(database, PhotoStore(context))
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun savesTasksWithEverything() = runTest {
        val due = Instant.parse("2026-09-24T06:30:00Z")
        val task = Task(
            title = "Read",
            note = "Chapter 3",
            dueDate = due,
            hasTime = true,
            priority = Priority.High,
            repeatRule = RepeatRule(RepeatRule.Frequency.Weekdays, weekdays = setOf(7, 2)),
            subtasks = listOf(Subtask(title = "Pages 1-20", sortIndex = 0), Subtask(title = "Notes", isDone = true, sortIndex = 1)),
        )
        store.save(task)

        val loaded = store.task(task.id)!!
        assertThat(loaded).isEqualTo(task.copy(createdAt = Instant.ofEpochMilli(task.createdAt.toEpochMilli())))
    }

    @Test
    fun savingDropsSubtasksThatWereRemoved() = runTest {
        val task = Task(title = "Trip", subtasks = listOf(Subtask(title = "Passport"), Subtask(title = "Tickets", sortIndex = 1)))
        store.save(task)
        store.save(task.copy(subtasks = task.subtasks.take(1)))

        assertThat(store.task(task.id)!!.subtasks.map { it.title }).containsExactly("Passport")
    }

    @Test
    fun deletingAListDeletesItsTasks() = runTest {
        val list = TaskList(name = "Work")
        store.saveLists(listOf(list))
        val inList = Task(title = "Report", listId = list.id)
        val inbox = Task(title = "Milk")
        store.save(inList, inbox)

        val removed = store.deleteList(list.id)

        assertThat(removed).containsExactly(inList.id)
        assertThat(store.tasks.first().map { it.title }).containsExactly("Milk")
    }
}
