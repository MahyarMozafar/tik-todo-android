package com.mahyarmozafar.tik

import com.google.common.truth.Truth.assertThat
import com.mahyarmozafar.tik.logic.TaskFilter
import com.mahyarmozafar.tik.model.Priority
import com.mahyarmozafar.tik.model.Subtask
import com.mahyarmozafar.tik.model.TaskSortOrder
import org.junit.Test

class TaskFilterTest {
    /** Thursday, September 24, 2026, noon. */
    private val now = date(2026, 9, 24, 12)

    @Test
    fun todayHasLateAndDueTasksButNotFutureOnes() {
        val tasks = listOf(
            makeTask("late", due = date(2026, 9, 22)),
            makeTask("today", due = date(2026, 9, 24)),
            makeTask("future", due = date(2026, 9, 25)),
            makeTask("no date"),
        )
        val result = TaskFilter.today(tasks, now, gregorian, TaskSortOrder.Time)
        assertThat(result.map { it.title }).containsExactly("late", "today").inOrder()
    }

    @Test
    fun doneTasksStayUntilTheDayEnds() {
        val doneToday = makeTask("done today", due = date(2026, 9, 24))
            .copy(isDone = true, completedAt = date(2026, 9, 24, 9))
        val doneYesterday = makeTask("done yesterday", due = date(2026, 9, 23))
            .copy(isDone = true, completedAt = date(2026, 9, 23, 18))

        val tasks = listOf(doneToday, doneYesterday)
        assertThat(TaskFilter.today(tasks, now, gregorian, TaskSortOrder.Time).map { it.title })
            .containsExactly("done today")
        assertThat(TaskFilter.today(tasks, now, gregorian, TaskSortOrder.Time, includeCompleted = false)).isEmpty()
    }

    @Test
    fun sortingByTimePutsTimedTasksFirstAndDoneTasksLast() {
        val done = makeTask("done", due = date(2026, 9, 24)).copy(isDone = true, completedAt = now)
        val noon = makeTask("noon", due = date(2026, 9, 24, 12), hasTime = true)
        val morning = makeTask("morning", due = date(2026, 9, 24, 8), hasTime = true)
        val anytime = makeTask("anytime", due = date(2026, 9, 24), priority = Priority.High)

        val sorted = TaskFilter.sorted(listOf(done, noon, anytime, morning), TaskSortOrder.Time, now, gregorian)
        assertThat(sorted.map { it.title }).containsExactly("morning", "noon", "anytime", "done").inOrder()
    }

    @Test
    fun sortingByPriorityPutsImportantTasksFirst() {
        val low = makeTask("low", due = date(2026, 9, 24, 8), hasTime = true, priority = Priority.Low)
        val high = makeTask("high", due = date(2026, 9, 24), priority = Priority.High)
        val none = makeTask("none", due = date(2026, 9, 24, 7), hasTime = true)

        val sorted = TaskFilter.sorted(listOf(low, none, high), TaskSortOrder.Priority, now, gregorian)
        assertThat(sorted.map { it.title }).containsExactly("high", "low", "none").inOrder()
    }

    @Test
    fun sortingByNewestPutsTheLatestAddedFirst() {
        val old = makeTask("old", createdAt = date(2026, 9, 20))
        val new = makeTask("new", createdAt = date(2026, 9, 23))
        val sorted = TaskFilter.sorted(listOf(old, new), TaskSortOrder.Newest, now, gregorian)
        assertThat(sorted.map { it.title }).containsExactly("new", "old").inOrder()
    }

    @Test
    fun lateTasksAlwaysComeFirst() {
        val early = makeTask("early today", due = date(2026, 9, 24, 6), hasTime = true)
        val late = makeTask("from yesterday", due = date(2026, 9, 23))

        val sorted = TaskFilter.sorted(listOf(early, late), TaskSortOrder.Time, now, gregorian)
        assertThat(sorted.map { it.title }).containsExactly("from yesterday", "early today").inOrder()
    }

    @Test
    fun scheduledGroupsUpcomingTasksByDay() {
        val a = makeTask("a", due = date(2026, 9, 25, 9), hasTime = true)
        val b = makeTask("b", due = date(2026, 9, 27))
        val c = makeTask("c", due = date(2026, 9, 25))
        val today = makeTask("today", due = date(2026, 9, 24))

        val groups = TaskFilter.scheduled(listOf(a, b, c, today), now, gregorian, TaskSortOrder.Time)
        assertThat(groups.map { it.day }).containsExactly(date(2026, 9, 25), date(2026, 9, 27)).inOrder()
        assertThat(groups[0].tasks.map { it.title }).containsExactly("a", "c").inOrder()
    }

    @Test
    fun completedGroupsByTheDayTasksWereDone() {
        val first = makeTask("first").copy(isDone = true, completedAt = date(2026, 9, 22, 10))
        val second = makeTask("second").copy(isDone = true, completedAt = date(2026, 9, 24, 8))
        val third = makeTask("third").copy(isDone = true, completedAt = date(2026, 9, 24, 11))

        val groups = TaskFilter.completed(listOf(first, second, third, makeTask("open")), gregorian)
        assertThat(groups.map { it.day }).containsExactly(date(2026, 9, 24), date(2026, 9, 22)).inOrder()
        assertThat(groups[0].tasks.map { it.title }).containsExactly("third", "second").inOrder()
    }

    @Test
    fun searchLooksAtTitlesNotesAndSubtasks() {
        val task = makeTask("Groceries").copy(note = "Call the bank first", subtasks = listOf(Subtask(title = "Eggs")))

        assertThat(TaskFilter.matches(task, "grocer")).isTrue()
        assertThat(TaskFilter.matches(task, "BANK")).isTrue()
        assertThat(TaskFilter.matches(task, "eggs")).isTrue()
        assertThat(TaskFilter.matches(task, "milk")).isFalse()
        assertThat(TaskFilter.matches(task, "   ")).isFalse()
    }

    @Test
    fun searchIgnoresArabicLetterForms() {
        // The same word typed with an Arabic keyboard (Arabic kaf and yeh) still matches.
        val task = makeTask("کتاب خواندنی")
        assertThat(TaskFilter.matches(task, "كتاب")).isTrue()
        assertThat(TaskFilter.matches(task, "خواندني")).isTrue()
    }
}
