package com.mahyarmozafar.tik

import com.google.common.truth.Truth.assertThat
import com.ibm.icu.util.Calendar
import com.ibm.icu.util.TimeZone
import com.ibm.icu.util.ULocale
import com.mahyarmozafar.tik.time.Shamsi
import com.mahyarmozafar.tik.time.ShamsiDate
import org.junit.Test
import java.time.LocalDate

class ShamsiTest {
    @Test
    fun knownDays() {
        assertThat(ShamsiDate.of(LocalDate.of(2026, 3, 21))).isEqualTo(ShamsiDate(1405, 1, 1))
        assertThat(ShamsiDate.of(LocalDate.of(2026, 9, 24))).isEqualTo(ShamsiDate(1405, 7, 2))
        assertThat(ShamsiDate.of(LocalDate.of(2025, 3, 20))).isEqualTo(ShamsiDate(1403, 12, 30))
        assertThat(ShamsiDate(1403, 12, 30).toLocalDate()).isEqualTo(LocalDate.of(2025, 3, 20))
    }

    @Test
    fun leapYears() {
        assertThat(Shamsi.isLeapYear(1403)).isTrue()
        assertThat(Shamsi.isLeapYear(1404)).isFalse()
        assertThat(Shamsi.monthLength(1404, 12)).isEqualTo(29)
        assertThat(Shamsi.monthLength(1405, 7)).isEqualTo(30)
    }

    @Test
    fun steppingMonthsKeepsTheDayWhenItCan() {
        assertThat(ShamsiDate(1405, 6, 31).plusMonths(1)).isEqualTo(ShamsiDate(1405, 7, 30))
        assertThat(ShamsiDate(1405, 12, 15).plusMonths(1)).isEqualTo(ShamsiDate(1406, 1, 15))
        assertThat(ShamsiDate(1403, 12, 30).plusYears(1)).isEqualTo(ShamsiDate(1404, 12, 29))
    }

    /** iOS uses ICU's Persian calendar, so both apps must agree with it on every day. */
    @Test
    fun agreesWithIcuFor200Years() {
        val icu = Calendar.getInstance(TimeZone.GMT_ZONE, ULocale("en@calendar=persian"))
        var day = LocalDate.of(1900, 1, 1)
        val end = LocalDate.of(2100, 12, 31)
        while (!day.isAfter(end)) {
            icu.timeInMillis = day.toEpochDay() * 86_400_000L + 43_200_000L
            val expected = ShamsiDate(
                icu.get(Calendar.EXTENDED_YEAR),
                icu.get(Calendar.MONTH) + 1,
                icu.get(Calendar.DAY_OF_MONTH),
            )
            val shamsi = ShamsiDate.of(day)
            assertThat(shamsi).isEqualTo(expected)
            assertThat(shamsi.toLocalDate()).isEqualTo(day)
            day = day.plusDays(1)
        }
    }
}
