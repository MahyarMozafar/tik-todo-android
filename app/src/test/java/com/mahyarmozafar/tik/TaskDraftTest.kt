package com.mahyarmozafar.tik

import com.google.common.truth.Truth.assertThat
import com.mahyarmozafar.tik.logic.SubtaskDraft
import com.mahyarmozafar.tik.logic.TaskDraft
import com.mahyarmozafar.tik.model.RepeatRule
import com.mahyarmozafar.tik.model.Subtask
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TaskDraftTest {
    private val now = date(2026, 9, 24, 15)

    @Test
    fun dueDateJoinsTheDayAndTheTime() {
        var draft = TaskDraft.forNew(gregorian, now).copy(
            hasDate = true,
            day = LocalDate.of(2026, 9, 24),
            hasTime = true,
            time = LocalTime.of(7, 30),
        )
        assertThat(draft.dueDate(gregorian)).isEqualTo(date(2026, 9, 24, 7, 30))

        draft = draft.copy(hasTime = false)
        assertThat(draft.dueDate(gregorian)).isEqualTo(date(2026, 9, 24))

        draft = draft.copy(hasDate = false)
        assertThat(draft.dueDate(gregorian)).isNull()
    }

    @Test
    fun applyAddsChangesAndRemovesSubtasks() {
        val task = makeTask("Trip").copy(
            subtasks = listOf(Subtask(title = "Passport", sortIndex = 0), Subtask(title = "Tickets", sortIndex = 1)),
        )

        var draft = TaskDraft.forTask(task, gregorian, now)
        draft = draft.copy(
            subtasks = listOf(
                draft.subtasks[0].copy(isDone = true),
                SubtaskDraft(title = "Bag"),
                SubtaskDraft(title = "   "),
            ),
        )
        val saved = draft.applyTo(task, gregorian, now)

        assertThat(saved.subtasks.map { it.title }).containsExactly("Passport", "Bag").inOrder()
        assertThat(saved.subtasks.first().isDone).isTrue()
        assertThat(saved.subtasks.first().id).isEqualTo(task.subtasks.first().id)
    }

    @Test
    fun removingTheDateAlsoRemovesTheRepeat() {
        val task = makeTask("Water plants", due = date(2026, 9, 24))
            .copy(repeatRule = RepeatRule(RepeatRule.Frequency.Daily))

        val saved = TaskDraft.forTask(task, gregorian, now).copy(hasDate = false).applyTo(task, gregorian, now)

        assertThat(saved.dueDate).isNull()
        assertThat(saved.repeatRule).isNull()
    }

    @Test
    fun monthlyRepeatRemembersTheDayOfTheMonth() {
        val task = makeTask("Pay rent")
        val saved = TaskDraft.forTask(task, gregorian, now).copy(
            hasDate = true,
            day = LocalDate.of(2026, 1, 31),
            repeatRule = RepeatRule(RepeatRule.Frequency.Monthly),
        ).applyTo(task, gregorian, now)

        assertThat(saved.repeatRule?.dayOfMonth).isEqualTo(31)
    }

    @Test
    fun titleIsTrimmedAndRequired() {
        var draft = TaskDraft.forNew(gregorian, now, title = "  Buy milk  ")
        assertThat(draft.trimmedTitle).isEqualTo("Buy milk")
        assertThat(draft.canSave).isTrue()

        draft = draft.copy(title = "   ")
        assertThat(draft.canSave).isFalse()
    }

    @Test
    fun survivesSavingAsText() {
        val draft = TaskDraft.forNew(gregorian, now, title = "Trip").copy(
            hasDate = true,
            hasTime = true,
            time = LocalTime.of(7, 30),
            repeatRule = RepeatRule(RepeatRule.Frequency.Weekdays, weekdays = setOf(7, 2)),
            subtasks = listOf(SubtaskDraft(title = "Passport", isDone = true)),
        )
        assertThat(TaskDraft.fromJson(draft.toJson())).isEqualTo(draft)
    }

    @Test
    fun newTasksGuessTheNextFullHour() {
        assertThat(TaskDraft.nextFullHour(date(2026, 9, 24, 15, 40), gregorian)).isEqualTo(LocalTime.of(16, 0))
        assertThat(TaskDraft.nextFullHour(date(2026, 9, 24, 23, 10), gregorian)).isEqualTo(LocalTime.of(0, 0))
    }
}
