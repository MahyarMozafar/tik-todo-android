package com.mahyarmozafar.tik

import com.mahyarmozafar.tik.model.CalendarKind
import com.mahyarmozafar.tik.model.Priority
import com.mahyarmozafar.tik.model.Task
import com.mahyarmozafar.tik.time.TikCalendar
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** A fixed time zone, so the tests give the same result on every computer. */
val testZone: ZoneId = ZoneId.of("Asia/Tehran")

val gregorian = TikCalendar(CalendarKind.Gregorian, testZone, firstWeekday = 1)
val persian = TikCalendar(CalendarKind.Persian, testZone, firstWeekday = 7)

fun date(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Instant =
    LocalDateTime.of(year, month, day, hour, minute).atZone(testZone).toInstant()

fun makeTask(
    title: String,
    due: Instant? = null,
    hasTime: Boolean = false,
    priority: Priority = Priority.None,
    createdAt: Instant = date(2026, 9, 1),
): Task = Task(title = title, dueDate = due, hasTime = hasTime, priority = priority, createdAt = createdAt)
