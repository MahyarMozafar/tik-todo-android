package com.mahyarmozafar.tik.app

import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.mahyarmozafar.tik.TikApplication
import com.mahyarmozafar.tik.logic.TaskFilter
import com.mahyarmozafar.tik.model.AccentChoice
import com.mahyarmozafar.tik.model.AppLanguage
import com.mahyarmozafar.tik.model.AppTheme
import com.mahyarmozafar.tik.model.CalendarKind
import com.mahyarmozafar.tik.model.ListIcon
import com.mahyarmozafar.tik.model.Priority
import com.mahyarmozafar.tik.model.RepeatRule
import com.mahyarmozafar.tik.model.Subtask
import com.mahyarmozafar.tik.model.Task
import com.mahyarmozafar.tik.model.TaskList
import com.mahyarmozafar.tik.time.TikCalendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.Instant

/**
 * Example content for screenshots and quick testing. Debug builds only.
 *
 * Launch extras (`adb shell am start ... --ez demo true --es lang fa`):
 * - `demo`: replace all data with example tasks, and start from the default settings
 * - `lang en|fa`, `theme system|light|dark`, `accent blue|purple|…`, `calendar persian|gregorian`
 * - `tab today|lists|search`: open on a tab
 * - `screen settings|editor|scheduled|list`: open a screen on top
 * - `add`: open the quick add field
 * - `celebrate`: tick everything due today, the last one a moment after launch, for the confetti
 */
object DemoData {
    fun applyLaunchExtras(intent: Intent?, app: TikApplication) {
        val extras = intent?.extras ?: return
        val model = app.model
        runBlocking {
            if (extras.getBoolean("demo")) model.resetSettings()
            val language = AppLanguage.fromTag(extras.getString("lang"))
            val theme = AppTheme.fromKey(extras.getString("theme"))
            val accent = AccentChoice.fromKey(extras.getString("accent"))
            val calendar = CalendarKind.fromKey(extras.getString("calendar"))
            model.updateSettings { settings ->
                settings.copy(
                    language = language ?: settings.language,
                    theme = theme ?: settings.theme,
                    accent = accent ?: settings.accent,
                    calendar = calendar ?: settings.calendar,
                )
            }
            if (language != null) AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))

            when (extras.getString("tab")) {
                "lists" -> model.selectedTab.value = AppTab.Lists
                "search" -> model.selectedTab.value = AppTab.Search
                "today" -> model.selectedTab.value = AppTab.Today
            }

            if (extras.getBoolean("demo")) {
                val settings = model.currentSettings()
                val calendar = settings.formatting().calendar
                val now = Instant.now()
                val (lists, demoTasks) = content(settings.language, calendar, now)
                var tasks = demoTasks
                if (extras.getBoolean("celebrate")) {
                    val today = TaskFilter.today(tasks, now, calendar, settings.sortOrder).filter { !it.isDone }.map { it.id }
                    val last = today.lastOrNull()
                    tasks = tasks.map { if (it.id in today && it.id != last) it.copy(isDone = true, completedAt = now) else it }
                    if (last != null) {
                        model.launch {
                            delay(1500)
                            toggle(last)
                        }
                    }
                }
                model.replaceAll(lists, tasks)
            }
            model.startScreen.value = extras.getString("screen")
            if (extras.getBoolean("add")) model.pendingQuickAdd.value = true
        }
    }

    fun content(language: AppLanguage, calendar: TikCalendar, now: Instant): Pair<List<TaskList>, List<Task>> {
        val text = if (language == AppLanguage.Farsi) Texts.farsi else Texts.english
        val today = calendar.localDate(now)
        fun day(offset: Long) = calendar.startOfDay(today.plusDays(offset))
        fun at(offset: Long, hour: Int, minute: Int = 0) = calendar.at(today.plusDays(offset), hour, minute)

        val personal = TaskList(name = text.personal, icon = ListIcon.House, color = AccentChoice.Blue, sortIndex = 0)
        val work = TaskList(name = text.work, icon = ListIcon.Briefcase, color = AccentChoice.Orange, sortIndex = 1)
        val shopping = TaskList(name = text.shopping, icon = ListIcon.Cart, color = AccentChoice.Green, sortIndex = 2)
        val health = TaskList(name = text.health, icon = ListIcon.Heart, color = AccentChoice.Pink, sortIndex = 3)

        var created = now.minusSeconds(3600L * 24 * 3)
        fun task(
            title: String,
            due: Instant? = null,
            hasTime: Boolean = false,
            list: TaskList? = null,
            priority: Priority = Priority.None,
            repeats: RepeatRule? = null,
            doneAt: Instant? = null,
            note: String = "",
            subtasks: List<Pair<String, Boolean>> = emptyList(),
        ): Task {
            created = created.plusSeconds(60)
            return Task(
                title = title,
                note = note,
                isDone = doneAt != null,
                completedAt = doneAt,
                createdAt = created,
                dueDate = due,
                hasTime = hasTime,
                priority = priority,
                repeatRule = repeats,
                listId = list?.id,
                subtasks = subtasks.mapIndexed { index, (name, done) -> Subtask(title = name, isDone = done, sortIndex = index) },
            )
        }

        val tasks = listOf(
            task(text.run, due = at(0, 7), hasTime = true, list = health, repeats = RepeatRule(RepeatRule.Frequency.Daily), doneAt = at(0, 7, 40)),
            task(text.email, due = at(0, 10), hasTime = true, list = work, priority = Priority.High),
            task(text.callMom, due = day(0), list = personal, priority = Priority.Medium),
            task(text.groceries, due = day(0), list = shopping, subtasks = listOf(text.milk to true, text.bread to false, text.eggs to false)),
            task(text.read, due = at(0, 21, 30), hasTime = true, list = personal, repeats = RepeatRule(RepeatRule.Frequency.Daily)),
            task(text.plants, due = day(0), list = personal, doneAt = at(0, 9, 10)),
            task(text.bill, due = day(-1), list = personal, priority = Priority.High),
            task(text.meeting, due = at(1, 11), hasTime = true, list = work, note = text.meetingNote),
            task(text.gym, due = at(1, 18), hasTime = true, list = health, repeats = RepeatRule(RepeatRule.Frequency.Weekdays, weekdays = setOf(7, 2, 4))),
            task(text.dentist, due = at(3, 16, 30), hasTime = true, list = health),
            task(text.trip, due = day(6), list = personal),
            task(text.learn),
            task(text.desk),
            task(text.portfolio, due = day(-1), list = work, doneAt = at(-1, 20)),
        )
        return listOf(personal, work, shopping, health) to tasks
    }

    private class Texts(
        val personal: String, val work: String, val shopping: String, val health: String,
        val run: String, val email: String, val callMom: String, val groceries: String,
        val milk: String, val bread: String, val eggs: String, val read: String, val plants: String, val bill: String,
        val meeting: String, val meetingNote: String, val gym: String, val dentist: String, val trip: String,
        val learn: String, val desk: String, val portfolio: String,
    ) {
        companion object {
            val english = Texts(
                personal = "Personal", work = "Work", shopping = "Shopping", health = "Health",
                run = "Morning run", email = "Reply to Sara's email", callMom = "Call mom",
                groceries = "Buy groceries", milk = "Milk", bread = "Bread", eggs = "Eggs",
                read = "Read 20 pages", plants = "Water the plants", bill = "Pay the internet bill",
                meeting = "Team meeting", meetingNote = "Bring the new designs.",
                gym = "Gym", dentist = "Dentist appointment", trip = "Plan the weekend trip",
                learn = "Learn Jetpack Compose", desk = "Clean the desk", portfolio = "Finish the portfolio site",
            )

            val farsi = Texts(
                personal = "شخصی", work = "کاری", shopping = "خرید", health = "سلامتی",
                run = "دویدن صبحگاهی", email = "جواب ایمیل سارا", callMom = "تماس با مامان",
                groceries = "خرید خانه", milk = "شیر", bread = "نان", eggs = "تخم‌مرغ",
                read = "خواندن 20 صفحه کتاب", plants = "آب دادن به گلدان‌ها", bill = "پرداخت قبض اینترنت",
                meeting = "جلسه‌ی تیم", meetingNote = "طرح‌های جدید را بیاور.",
                gym = "باشگاه", dentist = "وقت دندان‌پزشکی", trip = "برنامه‌ریزی سفر آخر هفته",
                learn = "یادگیری ⁦Jetpack Compose⁩", desk = "مرتب کردن میز", portfolio = "تمام کردن سایت نمونه‌کار",
            )
        }
    }
}
