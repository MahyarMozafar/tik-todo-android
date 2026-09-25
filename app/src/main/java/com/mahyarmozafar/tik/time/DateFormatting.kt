package com.mahyarmozafar.tik.time

import com.mahyarmozafar.tik.model.AppLanguage
import com.mahyarmozafar.tik.model.CalendarKind
import java.time.Instant
import java.time.ZoneId

/**
 * Turns dates into text the way the user picked in Settings: Shamsi or Gregorian, English or
 * Farsi words, and always English digits (for example "Wednesday, Mehr 2" or "07:30").
 *
 * The words and patterns match what iOS writes, so both apps read the same.
 */
class DateFormatting(
    val language: AppLanguage,
    val calendarKind: CalendarKind,
    val use24Hour: Boolean,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    /** Weeks start on Saturday in Iran. */
    val calendar = TikCalendar(
        kind = calendarKind,
        zone = zone,
        firstWeekday = if (calendarKind == CalendarKind.Persian || language == AppLanguage.Farsi) 7 else 1,
    )

    private val words: Words = if (language == AppLanguage.Farsi) Words.Farsi else Words.English
    private val farsi = language == AppLanguage.Farsi

    private data class DayParts(val year: Int, val month: Int, val day: Int, val weekday: Int)

    private fun parts(instant: Instant): DayParts {
        val date = calendar.localDate(instant)
        val month = calendar.month(date)
        return DayParts(month.year, month.month, calendar.dayOfMonth(date), TikCalendar.weekdayNumber(date))
    }

    private fun monthName(month: Int): String = words.months(calendarKind)[month - 1]

    private fun shortMonthName(month: Int): String = words.shortMonths(calendarKind)[month - 1]

    /** "Wednesday, Mehr 2" */
    fun fullDay(instant: Instant): String = parts(instant).let {
        if (farsi) "${words.weekdays[it.weekday - 1]} ${it.day} ${monthName(it.month)}"
        else "${words.weekdays[it.weekday - 1]}, ${monthName(it.month)} ${it.day}"
    }

    /** "Wednesday" */
    fun weekday(instant: Instant): String = words.weekdays[parts(instant).weekday - 1]

    /** "Mehr 2" */
    fun dayAndMonth(instant: Instant): String = parts(instant).let {
        if (farsi) "${it.day} ${monthName(it.month)}" else "${monthName(it.month)} ${it.day}"
    }

    /** "Sat, Mehr 5" */
    fun shortDay(instant: Instant): String = parts(instant).let {
        val weekday = words.shortWeekdays[it.weekday - 1]
        if (farsi) "$weekday ${it.day} ${shortMonthName(it.month)}"
        else "$weekday, ${shortMonthName(it.month)} ${it.day}"
    }

    /** "Mehr 5, 1405" */
    fun dayMonthYear(instant: Instant): String = parts(instant).let {
        if (farsi) "${it.day} ${shortMonthName(it.month)} ${it.year}"
        else "${shortMonthName(it.month)} ${it.day}, ${it.year}"
    }

    /** The words for the morning and the afternoon, for 12-hour time. */
    val amPm: Pair<String, String> get() = words.am to words.pm

    /** "Mehr 1405", for the title of a month in the date picker. */
    fun monthAndYear(month: CalendarMonth): String = "${monthName(month.month)} ${month.year}"

    /** "07:30", or "7:30 AM" when 24-hour time is off. */
    fun time(instant: Instant): String {
        val time = calendar.localTime(instant)
        val minute = time.minute.toString().padStart(2, '0')
        if (use24Hour) return "${time.hour.toString().padStart(2, '0')}:$minute"
        val hour = if (time.hour % 12 == 0) 12 else time.hour % 12
        return "$hour:$minute ${if (time.hour < 12) words.am else words.pm}"
    }

    /** "Today", "Tomorrow", "Yesterday", a weekday for the next few days, or a short date. */
    fun relativeDay(instant: Instant, now: Instant = Instant.now()): String =
        when (val distance = calendar.daysBetween(now, instant)) {
            0L -> words.today
            1L -> words.tomorrow
            -1L -> words.yesterday
            in 2L..6L -> weekday(instant)
            else -> if (calendar.isSameYear(instant, now)) shortDay(instant) else dayMonthYear(instant)
        }

    data class Weekday(val number: Int, val shortName: String, val letter: String)

    /** The days of the week in the order the week starts. Number 1 is Sunday. */
    val orderedWeekdays: List<Weekday>
        get() = (0 until 7).map { offset ->
            val number = (calendar.firstWeekday - 1 + offset) % 7 + 1
            Weekday(number, words.shortWeekdays[number - 1], words.letters[number - 1])
        }

    private class Words(
        val weekdays: List<String>,
        val shortWeekdays: List<String>,
        val letters: List<String>,
        val shamsiMonths: List<String>,
        val gregorianMonths: List<String>,
        val gregorianShortMonths: List<String>,
        val am: String,
        val pm: String,
        val today: String,
        val tomorrow: String,
        val yesterday: String,
    ) {
        fun months(kind: CalendarKind) = if (kind == CalendarKind.Persian) shamsiMonths else gregorianMonths

        fun shortMonths(kind: CalendarKind) = if (kind == CalendarKind.Persian) shamsiMonths else gregorianShortMonths

        companion object {
            val English = Words(
                weekdays = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"),
                shortWeekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"),
                letters = listOf("S", "M", "T", "W", "T", "F", "S"),
                shamsiMonths = listOf(
                    "Farvardin", "Ordibehesht", "Khordad", "Tir", "Mordad", "Shahrivar",
                    "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand",
                ),
                gregorianMonths = listOf(
                    "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December",
                ),
                gregorianShortMonths = listOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
                ),
                am = "AM",
                pm = "PM",
                today = "Today",
                tomorrow = "Tomorrow",
                yesterday = "Yesterday",
            )

            val Farsi = Words(
                weekdays = listOf("یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه"),
                shortWeekdays = listOf("یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه"),
                letters = listOf("ی", "د", "س", "چ", "پ", "ج", "ش"),
                shamsiMonths = listOf(
                    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
                    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
                ),
                gregorianMonths = listOf(
                    "ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن",
                    "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر",
                ),
                gregorianShortMonths = listOf(
                    "ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن",
                    "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر",
                ),
                am = "ق.ظ.",
                pm = "ب.ظ.",
                today = "امروز",
                tomorrow = "فردا",
                yesterday = "دیروز",
            )
        }
    }
}
