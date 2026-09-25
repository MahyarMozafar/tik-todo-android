package com.mahyarmozafar.tik.model

import com.mahyarmozafar.tik.time.DateFormatting
import java.time.ZoneId

enum class AppLanguage(val tag: String) {
    English("en"),
    Farsi("fa");

    val isRtl: Boolean get() = this == Farsi

    /** Each language is written in itself, so it is easy to find. */
    val nativeName: String
        get() = when (this) {
            English -> "English"
            Farsi -> "فارسی"
        }

    companion object {
        fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }
    }
}

enum class CalendarKind(val key: String) {
    Persian("persian"),
    Gregorian("gregorian");

    companion object {
        fun fromKey(key: String?): CalendarKind? = entries.firstOrNull { it.key == key }
    }
}

enum class AppTheme(val key: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    companion object {
        fun fromKey(key: String?): AppTheme? = entries.firstOrNull { it.key == key }
    }
}

/** The 11 colors used for the accent and for lists. */
enum class AccentChoice(val key: String) {
    Blue("blue"),
    Indigo("indigo"),
    Purple("purple"),
    Pink("pink"),
    Red("red"),
    Orange("orange"),
    Yellow("yellow"),
    Green("green"),
    Mint("mint"),
    Teal("teal"),
    Graphite("graphite");

    companion object {
        fun fromKey(key: String?): AccentChoice? = entries.firstOrNull { it.key == key }
    }
}

enum class TaskSortOrder(val key: String) {
    Time("time"),
    Priority("priority"),
    Newest("newest");

    companion object {
        fun fromKey(key: String?): TaskSortOrder? = entries.firstOrNull { it.key == key }
    }
}

/** The app icons to choose from. Each one is a launcher alias in the manifest. */
enum class AppIconChoice(val key: String, val aliasName: String) {
    Blue("blue", "com.mahyarmozafar.tik.LauncherBlue"),
    Midnight("midnight", "com.mahyarmozafar.tik.LauncherMidnight"),
    Light("light", "com.mahyarmozafar.tik.LauncherLight"),
    Mint("mint", "com.mahyarmozafar.tik.LauncherMint"),
    Purple("purple", "com.mahyarmozafar.tik.LauncherPurple"),
    Sunset("sunset", "com.mahyarmozafar.tik.LauncherSunset");

    companion object {
        fun fromKey(key: String?): AppIconChoice? = entries.firstOrNull { it.key == key }
    }
}

/** The icons a list can have. The key is what the database keeps. */
enum class ListIcon(val key: String) {
    List("list"),
    House("house"),
    Briefcase("briefcase"),
    Cart("cart"),
    Heart("heart"),
    Star("star"),
    Book("book"),
    School("school"),
    Dumbbell("dumbbell"),
    Run("run"),
    Food("food"),
    Cup("cup"),
    Airplane("airplane"),
    Car("car"),
    Gift("gift"),
    Flag("flag"),
    Bolt("bolt"),
    Leaf("leaf"),
    Paw("paw"),
    Gamepad("gamepad"),
    Music("music"),
    Brush("brush"),
    Laptop("laptop"),
    Code("code"),
    People("people"),
    Money("money"),
    Pills("pills"),
    Sparkles("sparkles"),
    Moon("moon"),
    Sun("sun");

    companion object {
        fun fromKey(key: String?): ListIcon = entries.firstOrNull { it.key == key } ?: List
    }
}

/** The built-in lists that collect tasks from every list. */
enum class SmartList {
    Today,
    Scheduled,
    All,
    Completed,
}

/** Which task details are used. Turned-off details are hidden everywhere. */
data class TaskFields(
    val notes: Boolean = true,
    val subtasks: Boolean = true,
    val dates: Boolean = true,
    val repeat: Boolean = true,
    val priority: Boolean = true,
    val photos: Boolean = true,
)

/** Everything that can be changed in Settings. */
data class TikSettings(
    val language: AppLanguage = AppLanguage.English,
    val calendar: CalendarKind = CalendarKind.Persian,
    val use24Hour: Boolean = true,
    val theme: AppTheme = AppTheme.System,
    val accent: AccentChoice = AccentChoice.Blue,
    val wallpaperColors: Boolean = false,
    val colorfulBackground: Boolean = true,
    val sortOrder: TaskSortOrder = TaskSortOrder.Time,
    val showProgress: Boolean = true,
    val showCompletedInToday: Boolean = true,
    val haptics: Boolean = true,
    val sounds: Boolean = true,
    val celebration: Boolean = true,
    val fields: TaskFields = TaskFields(),
    val reminderOffsetMinutes: Int = 0,
    val badge: Boolean = false,
    val appIcon: AppIconChoice = AppIconChoice.Blue,
) {
    fun formatting(zone: ZoneId = ZoneId.systemDefault()): DateFormatting = DateFormatting(language, calendar, use24Hour, zone)
}
