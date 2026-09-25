package com.mahyarmozafar.tik

import com.google.common.truth.Truth.assertThat
import com.mahyarmozafar.tik.logic.TaskActions
import com.mahyarmozafar.tik.model.Priority
import com.mahyarmozafar.tik.model.RepeatRule
import com.mahyarmozafar.tik.model.Subtask
import org.junit.Test

class TaskActionsTest {
    @Test
    fun tickingARepeatingTaskMakesTheNextOne() {
        val task = makeTask("Read", due = date(2026, 9, 24, 21, 30), hasTime = true).copy(
            repeatRule = RepeatRule(RepeatRule.Frequency.Daily),
            subtasks = listOf(Subtask(title = "Chapter 3", isDone = true)),
        )

        val result = TaskActions.toggle(task, listOf(task), gregorian, now = date(2026, 9, 24, 22))

        assertThat(result.isDone).isTrue()
        assertThat(result.task.completedAt).isEqualTo(date(2026, 9, 24, 22))
        val next = result.nextOccurrence!!
        assertThat(next.title).isEqualTo("Read")
        assertThat(next.dueDate).isEqualTo(date(2026, 9, 25, 21, 30))
        assertThat(next.repeatRule).isEqualTo(RepeatRule(RepeatRule.Frequency.Daily))
        assertThat(next.previousOccurrenceId).isEqualTo(task.id)
        // Subtasks come along, but not ticked.
        assertThat(next.subtasks.map { it.title }).containsExactly("Chapter 3")
        assertThat(next.subtasks.none { it.isDone }).isTrue()
    }

    @Test
    fun untickingRemovesTheCopyAgain() {
        val task = makeTask("Gym", due = date(2026, 9, 24)).copy(repeatRule = RepeatRule(RepeatRule.Frequency.Weekly))

        val ticked = TaskActions.toggle(task, listOf(task), gregorian, now = date(2026, 9, 24, 8))
        val next = ticked.nextOccurrence!!

        val unticked = TaskActions.toggle(ticked.task, listOf(ticked.task, next), gregorian, now = date(2026, 9, 24, 9))
        assertThat(unticked.isDone).isFalse()
        assertThat(unticked.task.completedAt).isNull()
        assertThat(unticked.removedIds).containsExactly(next.id)
    }

    @Test
    fun untickingKeepsACopyThatWasAlreadyDone() {
        val task = makeTask("Gym", due = date(2026, 9, 24)).copy(repeatRule = RepeatRule(RepeatRule.Frequency.Weekly))
        val ticked = TaskActions.toggle(task, listOf(task), gregorian, now = date(2026, 9, 24, 8))
        val doneCopy = ticked.nextOccurrence!!.copy(isDone = true, completedAt = date(2026, 10, 1))

        val unticked = TaskActions.toggle(ticked.task, listOf(ticked.task, doneCopy), gregorian, now = date(2026, 10, 2))
        assertThat(unticked.removedIds).isEmpty()
    }

    @Test
    fun tickingANormalTaskMakesNoCopy() {
        val task = makeTask("Call mom", due = date(2026, 9, 24))
        val result = TaskActions.toggle(task, listOf(task), gregorian, now = date(2026, 9, 24, 12))
        assertThat(result.isDone).isTrue()
        assertThat(result.nextOccurrence).isNull()
    }

    @Test
    fun moveToTomorrowKeepsTheTime() {
        val task = makeTask("Meeting", due = date(2026, 9, 24, 11, 15), hasTime = true)
        val moved = TaskActions.moveToTomorrow(task, gregorian, now = date(2026, 9, 24, 9))
        assertThat(moved.dueDate).isEqualTo(date(2026, 9, 25, 11, 15))
    }

    @Test
    fun duplicateMakesAnOpenCopy() {
        val task = makeTask("Pack", due = date(2026, 9, 24), priority = Priority.High)
            .copy(note = "Passport!", isDone = true, completedAt = date(2026, 9, 24, 8))

        val copy = TaskActions.duplicate(task, now = date(2026, 9, 24, 9))
        assertThat(copy.id).isNotEqualTo(task.id)
        assertThat(copy.title).isEqualTo("Pack")
        assertThat(copy.note).isEqualTo("Passport!")
        assertThat(copy.priority).isEqualTo(Priority.High)
        assertThat(copy.isDone).isFalse()
    }
}
