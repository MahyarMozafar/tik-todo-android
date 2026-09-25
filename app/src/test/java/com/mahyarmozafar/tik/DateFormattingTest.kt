package com.mahyarmozafar.tik

import com.google.common.truth.Truth.assertThat
import com.mahyarmozafar.tik.model.AppLanguage
import com.mahyarmozafar.tik.model.CalendarKind
import com.mahyarmozafar.tik.time.DateFormatting
import org.junit.Test

class DateFormattingTest {
    /** Thursday, September 24, 2026 is 2 Mehr 1405 in the Shamsi calendar. */
    private val day = date(2026, 9, 24, 7, 5)

    private fun formatting(language: AppLanguage, calendar: CalendarKind, use24Hour: Boolean = true) =
        DateFormatting(language, calendar, use24Hour, testZone)

    @Test
    fun shamsiInEnglish() {
        val formatting = formatting(AppLanguage.English, CalendarKind.Persian)
        assertThat(formatting.fullDay(day)).isEqualTo("Thursday, Mehr 2")
        assertThat(formatting.time(day)).isEqualTo("07:05")
        assertThat(formatting.dayMonthYear(day)).isEqualTo("Mehr 2, 1405")
    }

    @Test
    fun gregorianInEnglish() {
        val formatting = formatting(AppLanguage.English, CalendarKind.Gregorian)
        assertThat(formatting.fullDay(day)).isEqualTo("Thursday, September 24")
        assertThat(formatting.shortDay(day)).isEqualTo("Thu, Sep 24")
    }

    @Test
    fun twelveHourTime() {
        val formatting = formatting(AppLanguage.English, CalendarKind.Gregorian, use24Hour = false)
        assertThat(formatting.time(day)).startsWith("7:05")
        assertThat(formatting.time(day)).endsWith("AM")
        assertThat(formatting.time(date(2026, 9, 24, 0, 30))).startsWith("12:30")
        assertThat(formatting.time(date(2026, 9, 24, 12, 0))).endsWith("PM")
    }

    @Test
    fun farsiAlwaysUsesEnglishDigits() {
        val formatting = formatting(AppLanguage.Farsi, CalendarKind.Persian)
        val text = formatting.dayAndMonth(day) + formatting.time(day)
        assertThat(text).contains("2")
        assertThat(text.any { it in '۰'..'۹' || it in '٠'..'٩' }).isFalse()
    }

    @Test
    fun shamsiWeeksStartOnSaturday() {
        val formatting = formatting(AppLanguage.English, CalendarKind.Persian)
        assertThat(formatting.orderedWeekdays.map { it.number }).containsExactly(7, 1, 2, 3, 4, 5, 6).inOrder()
        val gregorianWeek = formatting(AppLanguage.English, CalendarKind.Gregorian)
        assertThat(gregorianWeek.orderedWeekdays.first().number).isEqualTo(1)
    }

    @Test
    fun relativeDays() {
        val formatting = formatting(AppLanguage.English, CalendarKind.Gregorian)
        assertThat(formatting.relativeDay(date(2026, 9, 24), now = day)).isEqualTo("Today")
        assertThat(formatting.relativeDay(date(2026, 9, 25), now = day)).isEqualTo("Tomorrow")
        assertThat(formatting.relativeDay(date(2026, 9, 23), now = day)).isEqualTo("Yesterday")
        assertThat(formatting.relativeDay(date(2026, 9, 27), now = day)).isEqualTo("Sunday")
        assertThat(formatting.relativeDay(date(2026, 10, 12), now = day)).isEqualTo("Mon, Oct 12")
        assertThat(formatting.relativeDay(date(2027, 1, 2), now = day)).isEqualTo("Jan 2, 2027")
    }
}
