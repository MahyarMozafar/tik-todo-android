package com.mahyarmozafar.tik

import com.google.common.truth.Truth.assertThat
import com.mahyarmozafar.tik.model.Priority
import com.mahyarmozafar.tik.model.TikSettings
import com.mahyarmozafar.tik.widget.widgetState
import org.junit.Test

class WidgetStateTest {
    /** Thursday, September 24, 2026, noon. */
    private val now = date(2026, 9, 24, 12)

    private val tasks = listOf(
        makeTask("evening run", due = date(2026, 9, 24, 19, 30), hasTime = true),
        makeTask("important", due = date(2026, 9, 24), priority = Priority.High),
        makeTask("morning call", due = date(2026, 9, 24, 9), hasTime = true),
        makeTask("tomorrow", due = date(2026, 9, 25)),
        makeTask("done", due = date(2026, 9, 24)).copy(isDone = true, completedAt = date(2026, 9, 24, 10)),
    )

    @Test
    fun showsTodaysTasksInOrderWithTheirTimes() {
        val state = widgetState(tasks, TikSettings(), now, testZone)
        assertThat(state.tasks.map { it.title }).containsExactly("morning call", "evening run", "important", "done").inOrder()
        assertThat(state.tasks.map { it.time }).containsExactly("09:00", "19:30", null, null).inOrder()
        assertThat(state.tasks.map { it.isLate }).containsExactly(true, false, false, false).inOrder()
        assertThat(state.doneCount).isEqualTo(1)
        assertThat(state.totalCount).isEqualTo(4)
        assertThat(state.openCount).isEqualTo(3)
        assertThat(state.progress).isEqualTo(0.25f)
    }

    @Test
    fun hidesDoneTasksWhenTodayDoes() {
        val state = widgetState(tasks, TikSettings(showCompletedInToday = false), now, testZone)
        assertThat(state.tasks.map { it.title }).containsExactly("morning call", "evening run", "important").inOrder()
        // The count still includes them, like the Today screen.
        assertThat(state.doneCount).isEqualTo(1)
        assertThat(state.totalCount).isEqualTo(4)
    }

    @Test
    fun timesFollowTheClockSetting() {
        val state = widgetState(tasks, TikSettings(use24Hour = false), now, testZone)
        // A no-break space keeps "PM" on the same line as the time.
        assertThat(state.tasks[1].time).isEqualTo("7:30\u00A0PM")
    }

    @Test
    fun anEmptyDayHasNoProgress() {
        val state = widgetState(emptyList(), TikSettings(), now, testZone)
        assertThat(state.tasks).isEmpty()
        assertThat(state.progress).isEqualTo(0f)
    }
}
