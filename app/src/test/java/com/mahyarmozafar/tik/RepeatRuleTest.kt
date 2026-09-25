package com.mahyarmozafar.tik

import com.google.common.truth.Truth.assertThat
import com.mahyarmozafar.tik.model.RepeatRule
import com.mahyarmozafar.tik.model.RepeatRule.Frequency
import com.mahyarmozafar.tik.time.ShamsiDate
import org.junit.Test

class RepeatRuleTest {
    /** Thursday, September 24, 2026, 10:00. */
    private val now = date(2026, 9, 24, 10)

    @Test
    fun dailyGoesToTomorrow() {
        val next = RepeatRule(Frequency.Daily).nextDueDate(date(2026, 9, 24), now, gregorian)
        assertThat(next).isEqualTo(date(2026, 9, 25))
    }

    @Test
    fun aLateDailyTaskComesBackTomorrowNotInThePast() {
        val next = RepeatRule(Frequency.Daily).nextDueDate(date(2026, 9, 20), now, gregorian)
        assertThat(next).isEqualTo(date(2026, 9, 25))
    }

    @Test
    fun keepsTheTimeOfDay() {
        val next = RepeatRule(Frequency.Daily).nextDueDate(date(2026, 9, 24, 7, 30), now, gregorian)
        assertThat(next).isEqualTo(date(2026, 9, 25, 7, 30))
    }

    @Test
    fun weeklyKeepsTheWeekday() {
        // Due on Monday, September 7, and ticked weeks later on a Thursday.
        val next = RepeatRule(Frequency.Weekly).nextDueDate(date(2026, 9, 7), now, gregorian)
        assertThat(next).isEqualTo(date(2026, 9, 28))
        assertThat(gregorian.weekday(next)).isEqualTo(2)
    }

    @Test
    fun certainWeekdaysPicksTheNextOne() {
        // Saturday, Monday and Wednesday. Ticked on Thursday, so Saturday is next.
        val rule = RepeatRule(Frequency.Weekdays, weekdays = setOf(7, 2, 4))
        assertThat(rule.nextDueDate(date(2026, 9, 24), now, gregorian)).isEqualTo(date(2026, 9, 26))
    }

    @Test
    fun everyFewDays() {
        val rule = RepeatRule(Frequency.EveryNDays, interval = 3)
        assertThat(rule.nextDueDate(date(2026, 9, 24), now, gregorian)).isEqualTo(date(2026, 9, 27))
    }

    @Test
    fun monthlyFromThe31stDoesNotDrift() {
        // Due January 31 and ticked in early March: the next one is March 31, not March 28.
        val next = RepeatRule(Frequency.Monthly).nextDueDate(date(2026, 1, 31), date(2026, 3, 5, 9), gregorian)
        assertThat(next).isEqualTo(date(2026, 3, 31))
    }

    @Test
    fun monthlyGoesBackToThe31stAfterAShortMonth() {
        // January 31 became February 28; the next one is March 31 again.
        val rule = RepeatRule(Frequency.Monthly, dayOfMonth = 31)
        assertThat(rule.nextDueDate(date(2026, 2, 28), date(2026, 2, 28, 9), gregorian)).isEqualTo(date(2026, 3, 31))

        // Without the remembered day, it would stay on the 28th.
        val plain = RepeatRule(Frequency.Monthly)
        assertThat(plain.nextDueDate(date(2026, 2, 28), date(2026, 2, 28, 9), gregorian)).isEqualTo(date(2026, 3, 28))
    }

    @Test
    fun monthlyWithShamsiFollowsShamsiMonths() {
        // 1 Mehr 1405 is followed by 1 Aban 1405.
        val due = persian.startOfDay(ShamsiDate(1405, 7, 1).toLocalDate())
        val next = RepeatRule(Frequency.Monthly).nextDueDate(due, due, persian)
        assertThat(ShamsiDate.of(persian.localDate(next))).isEqualTo(ShamsiDate(1405, 8, 1))
    }

    @Test
    fun survivesSavingAsJson() {
        val rule = RepeatRule(Frequency.Weekdays, weekdays = setOf(1, 3, 5), interval = 4)
        assertThat(RepeatRule.fromJson(rule.toJson())).isEqualTo(rule)
    }

    @Test
    fun readsTheSameJsonAsIos() {
        val rule = RepeatRule.fromJson("""{"frequency":"everyNDays","weekdays":[],"interval":3}""")
        assertThat(rule).isEqualTo(RepeatRule(Frequency.EveryNDays, interval = 3))
    }
}
