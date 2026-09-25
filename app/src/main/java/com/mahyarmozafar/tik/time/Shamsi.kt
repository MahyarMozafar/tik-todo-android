package com.mahyarmozafar.tik.time

import java.time.LocalDate
import kotlin.math.min

/** A day in the Shamsi (Solar Hijri) calendar. [month] is 1 to 12. */
data class ShamsiDate(val year: Int, val month: Int, val day: Int) {

    fun toLocalDate(): LocalDate = Shamsi.toLocalDate(year, month, day)

    /** Like a Gregorian month step: a day that does not exist becomes the month's last day. */
    fun plusMonths(months: Int): ShamsiDate {
        val total = year * 12L + (month - 1) + months
        val newYear = Math.floorDiv(total, 12L).toInt()
        val newMonth = Math.floorMod(total, 12L).toInt() + 1
        return ShamsiDate(newYear, newMonth, min(day, Shamsi.monthLength(newYear, newMonth)))
    }

    fun plusYears(years: Int): ShamsiDate {
        val newYear = year + years
        return ShamsiDate(newYear, month, min(day, Shamsi.monthLength(newYear, month)))
    }

    fun withDay(newDay: Int): ShamsiDate = copy(day = newDay.coerceIn(1, Shamsi.monthLength(year, month)))

    companion object {
        fun of(date: LocalDate): ShamsiDate = Shamsi.fromLocalDate(date)
    }
}

/**
 * Converts between Shamsi and Gregorian days.
 *
 * It uses the same 33-year arithmetic as ICU's Persian calendar, which iOS uses too,
 * so the Android and iOS apps agree on every date.
 */
object Shamsi {
    private const val PERSIAN_EPOCH = 1_948_320L
    private const val EPOCH_JULIAN_DAY = 2_440_588L
    private val daysBeforeMonth = intArrayOf(0, 31, 62, 93, 124, 155, 186, 216, 246, 276, 306, 336)

    fun isLeapYear(year: Int): Boolean = Math.floorMod(25L * year + 11, 33L) < 8

    fun monthLength(year: Int, month: Int): Int = when {
        month <= 6 -> 31
        month <= 11 -> 30
        isLeapYear(year) -> 30
        else -> 29
    }

    fun fromLocalDate(date: LocalDate): ShamsiDate {
        val daysSinceEpoch = date.toEpochDay() + EPOCH_JULIAN_DAY - PERSIAN_EPOCH
        val year = 1 + Math.floorDiv(33 * daysSinceEpoch + 3, 12_053L).toInt()
        val firstDayOfYear = 365L * (year - 1) + Math.floorDiv(8L * year + 21, 33L)
        val dayOfYear = (daysSinceEpoch - firstDayOfYear).toInt()
        val monthIndex = if (dayOfYear < 216) dayOfYear / 31 else (dayOfYear - 6) / 30
        return ShamsiDate(year, monthIndex + 1, dayOfYear - daysBeforeMonth[monthIndex] + 1)
    }

    fun toLocalDate(year: Int, month: Int, day: Int): LocalDate {
        require(month in 1..12) { "month must be 1 to 12, was $month" }
        val julianDay = PERSIAN_EPOCH - 1 + 365L * (year - 1) + Math.floorDiv(8L * year + 21, 33L) +
            daysBeforeMonth[month - 1] + day
        return LocalDate.ofEpochDay(julianDay - EPOCH_JULIAN_DAY)
    }
}
