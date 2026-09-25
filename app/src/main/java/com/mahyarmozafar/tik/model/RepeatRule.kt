package com.mahyarmozafar.tik.model

import com.mahyarmozafar.tik.time.TikCalendar
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import kotlin.math.max
import kotlin.math.min

/** How a task comes back after it is done. */
@Serializable
data class RepeatRule(
    val frequency: Frequency,
    /** Only for [Frequency.Weekdays]. 1 is Sunday, 7 is Saturday. */
    val weekdays: Set<Int> = emptySet(),
    /** Only for [Frequency.EveryNDays]. */
    val interval: Int = 2,
    /**
     * Only for monthly and yearly repeats: the day of the month the task started on.
     * A task on the 31st moves to the 28th in February, and this brings it back.
     */
    val dayOfMonth: Int? = null,
) {
    @Serializable
    enum class Frequency {
        @SerialName("daily") Daily,
        @SerialName("weekdays") Weekdays,
        @SerialName("weekly") Weekly,
        @SerialName("monthly") Monthly,
        @SerialName("yearly") Yearly,
        @SerialName("everyNDays") EveryNDays,
    }

    /**
     * When a repeating task is ticked, its next copy goes on this date. It is always after [due]
     * and never on a day that has already started, so a late task does not come back late again.
     */
    fun nextDueDate(due: Instant, now: Instant, calendar: TikCalendar): Instant {
        val startOfTomorrow = calendar.addDays(calendar.startOfDay(now), 1)

        val days = weekdays.filterTo(mutableSetOf()) { it in 1..7 }
        if (frequency == Frequency.Weekdays && days.isNotEmpty()) {
            var candidate = due
            do {
                candidate = calendar.addDays(candidate, 1)
            } while (candidate < startOfTomorrow || calendar.weekday(candidate) !in days)
            return candidate
        }

        var count = 1
        while (true) {
            val candidate = keepingDayOfMonth(step(due, count, calendar), calendar)
            if (candidate >= startOfTomorrow) return candidate
            count++
        }
    }

    private fun step(due: Instant, count: Int, calendar: TikCalendar): Instant = when (frequency) {
        Frequency.Daily, Frequency.Weekdays -> calendar.addDays(due, count.toLong())
        Frequency.Weekly -> calendar.addDays(due, 7L * count)
        Frequency.Monthly -> calendar.addMonths(due, count)
        Frequency.Yearly -> calendar.addYears(due, count)
        Frequency.EveryNDays -> calendar.addDays(due, max(1, interval).toLong() * count)
    }

    /** Moves a monthly or yearly date back to [dayOfMonth], as far as the month allows. */
    private fun keepingDayOfMonth(date: Instant, calendar: TikCalendar): Instant {
        if (frequency != Frequency.Monthly && frequency != Frequency.Yearly) return date
        val day = dayOfMonth ?: return date
        return calendar.withDayOfMonth(date, min(day, calendar.daysInMonth(date)))
    }

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(text: String?): RepeatRule? =
            text?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }
    }
}
