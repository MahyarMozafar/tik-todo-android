package com.mahyarmozafar.tik.time

import com.mahyarmozafar.tik.model.CalendarKind
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** A year and month in one of the two calendars. [month] is 1 to 12. */
data class CalendarMonth(val year: Int, val month: Int) {
    fun plus(months: Int): CalendarMonth {
        val total = year * 12L + (month - 1) + months
        return CalendarMonth(Math.floorDiv(total, 12L).toInt(), Math.floorMod(total, 12L).toInt() + 1)
    }
}

/**
 * Date math in the calendar picked in Settings (Shamsi or Gregorian), in the phone's time zone.
 *
 * Weekday numbers follow iOS, so saved repeat rules mean the same thing on both:
 * 1 is Sunday and 7 is Saturday.
 */
class TikCalendar(
    val kind: CalendarKind,
    val zone: ZoneId = ZoneId.systemDefault(),
    /** 7 when weeks start on Saturday, as they do in Iran. */
    val firstWeekday: Int = 1,
) {
    fun localDate(instant: Instant): LocalDate = instant.atZone(zone).toLocalDate()

    fun localTime(instant: Instant): LocalTime = instant.atZone(zone).toLocalTime()

    fun startOfDay(instant: Instant): Instant = startOfDay(localDate(instant))

    fun startOfDay(date: LocalDate): Instant = date.atStartOfDay(zone).toInstant()

    fun at(date: LocalDate, hour: Int, minute: Int): Instant =
        ZonedDateTime.of(date, LocalTime.of(hour, minute), zone).toInstant()

    fun addDays(instant: Instant, days: Long): Instant = instant.atZone(zone).plusDays(days).toInstant()

    fun addMonths(instant: Instant, months: Int): Instant = moveDay(instant) { date ->
        when (kind) {
            CalendarKind.Gregorian -> date.plusMonths(months.toLong())
            CalendarKind.Persian -> ShamsiDate.of(date).plusMonths(months).toLocalDate()
        }
    }

    fun addYears(instant: Instant, years: Int): Instant = moveDay(instant) { date ->
        when (kind) {
            CalendarKind.Gregorian -> date.plusYears(years.toLong())
            CalendarKind.Persian -> ShamsiDate.of(date).plusYears(years).toLocalDate()
        }
    }

    fun dayOfMonth(instant: Instant): Int = dayOfMonth(localDate(instant))

    fun dayOfMonth(date: LocalDate): Int = when (kind) {
        CalendarKind.Gregorian -> date.dayOfMonth
        CalendarKind.Persian -> ShamsiDate.of(date).day
    }

    fun daysInMonth(instant: Instant): Int = month(localDate(instant)).let { daysInMonth(it) }

    fun withDayOfMonth(instant: Instant, day: Int): Instant = moveDay(instant) { date ->
        when (kind) {
            CalendarKind.Gregorian -> date.withDayOfMonth(day.coerceIn(1, date.lengthOfMonth()))
            CalendarKind.Persian -> ShamsiDate.of(date).withDay(day).toLocalDate()
        }
    }

    fun weekday(instant: Instant): Int = weekdayNumber(localDate(instant))

    fun isSameDay(a: Instant, b: Instant): Boolean = localDate(a) == localDate(b)

    fun isSameYear(a: Instant, b: Instant): Boolean = month(localDate(a)).year == month(localDate(b)).year

    /** Whole days from the day of [from] to the day of [to]. */
    fun daysBetween(from: Instant, to: Instant): Long = ChronoUnit.DAYS.between(localDate(from), localDate(to))

    // Month grids for the date picker.

    fun month(date: LocalDate): CalendarMonth = when (kind) {
        CalendarKind.Gregorian -> CalendarMonth(date.year, date.monthValue)
        CalendarKind.Persian -> ShamsiDate.of(date).let { CalendarMonth(it.year, it.month) }
    }

    fun daysInMonth(month: CalendarMonth): Int = when (kind) {
        CalendarKind.Gregorian -> java.time.YearMonth.of(month.year, month.month).lengthOfMonth()
        CalendarKind.Persian -> Shamsi.monthLength(month.year, month.month)
    }

    fun date(month: CalendarMonth, day: Int): LocalDate = when (kind) {
        CalendarKind.Gregorian -> LocalDate.of(month.year, month.month, day)
        CalendarKind.Persian -> Shamsi.toLocalDate(month.year, month.month, day)
    }

    /** How many empty cells come before the 1st in a week row that starts on [firstWeekday]. */
    fun leadingBlankDays(month: CalendarMonth): Int {
        val first = weekdayNumber(date(month, 1))
        return Math.floorMod(first - firstWeekday, 7)
    }

    private inline fun moveDay(instant: Instant, transform: (LocalDate) -> LocalDate): Instant {
        val zoned = instant.atZone(zone)
        return ZonedDateTime.of(transform(zoned.toLocalDate()), zoned.toLocalTime(), zone).toInstant()
    }

    companion object {
        /** 1 is Sunday and 7 is Saturday. */
        fun weekdayNumber(date: LocalDate): Int = date.dayOfWeek.value % 7 + 1
    }
}
