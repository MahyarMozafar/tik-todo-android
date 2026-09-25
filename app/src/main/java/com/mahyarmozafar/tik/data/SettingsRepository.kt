package com.mahyarmozafar.tik.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mahyarmozafar.tik.model.AccentChoice
import com.mahyarmozafar.tik.model.AppIconChoice
import com.mahyarmozafar.tik.model.AppLanguage
import com.mahyarmozafar.tik.model.AppTheme
import com.mahyarmozafar.tik.model.CalendarKind
import com.mahyarmozafar.tik.model.TaskFields
import com.mahyarmozafar.tik.model.TaskSortOrder
import com.mahyarmozafar.tik.model.TikSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** Keeps [TikSettings]. The widget and the reminders read the same values as the app. */
class SettingsRepository(context: Context) {
    private val store = context.settingsStore

    val settings: Flow<TikSettings> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it.toSettings() }

    suspend fun current(): TikSettings = settings.first()

    suspend fun update(transform: (TikSettings) -> TikSettings) {
        store.edit { preferences -> preferences.write(transform(preferences.toSettings())) }
    }

    /** Returns true only the first time it is called, so starter lists are made once. */
    suspend fun claimStarterLists(): Boolean {
        var first = false
        store.edit { preferences ->
            if (preferences[Keys.starterListsMade] != true) {
                preferences[Keys.starterListsMade] = true
                first = true
            }
        }
        return first
    }

    /** Back to the default settings, so every demo run looks the same. */
    suspend fun reset() {
        store.edit { preferences ->
            val starterListsMade = preferences[Keys.starterListsMade]
            preferences.clear()
            starterListsMade?.let { preferences[Keys.starterListsMade] = it }
        }
    }

    private object Keys {
        val language = stringPreferencesKey("language")
        val calendar = stringPreferencesKey("calendar")
        val use24Hour = booleanPreferencesKey("use24Hour")
        val theme = stringPreferencesKey("theme")
        val accent = stringPreferencesKey("accent")
        val wallpaperColors = booleanPreferencesKey("wallpaperColors")
        val colorfulBackground = booleanPreferencesKey("colorfulBackground")
        val sortOrder = stringPreferencesKey("sortOrder")
        val showProgress = booleanPreferencesKey("showProgress")
        val showCompletedInToday = booleanPreferencesKey("showCompletedInToday")
        val haptics = booleanPreferencesKey("haptics")
        val sounds = booleanPreferencesKey("sounds")
        val celebration = booleanPreferencesKey("celebration")
        val fieldNotes = booleanPreferencesKey("field.notes")
        val fieldSubtasks = booleanPreferencesKey("field.subtasks")
        val fieldDates = booleanPreferencesKey("field.dates")
        val fieldRepeat = booleanPreferencesKey("field.repeat")
        val fieldPriority = booleanPreferencesKey("field.priority")
        val fieldPhotos = booleanPreferencesKey("field.photos")
        val reminderOffset = intPreferencesKey("reminderOffset")
        val badge = booleanPreferencesKey("badge")
        val appIcon = stringPreferencesKey("appIcon")
        val starterListsMade = booleanPreferencesKey("didCreateStarterLists")
    }

    private fun Preferences.toSettings(): TikSettings {
        val defaults = TikSettings()
        return TikSettings(
            language = AppLanguage.fromTag(this[Keys.language]) ?: defaults.language,
            calendar = CalendarKind.fromKey(this[Keys.calendar]) ?: defaults.calendar,
            use24Hour = this[Keys.use24Hour] ?: defaults.use24Hour,
            theme = AppTheme.fromKey(this[Keys.theme]) ?: defaults.theme,
            accent = AccentChoice.fromKey(this[Keys.accent]) ?: defaults.accent,
            wallpaperColors = this[Keys.wallpaperColors] ?: defaults.wallpaperColors,
            colorfulBackground = this[Keys.colorfulBackground] ?: defaults.colorfulBackground,
            sortOrder = TaskSortOrder.fromKey(this[Keys.sortOrder]) ?: defaults.sortOrder,
            showProgress = this[Keys.showProgress] ?: defaults.showProgress,
            showCompletedInToday = this[Keys.showCompletedInToday] ?: defaults.showCompletedInToday,
            haptics = this[Keys.haptics] ?: defaults.haptics,
            sounds = this[Keys.sounds] ?: defaults.sounds,
            celebration = this[Keys.celebration] ?: defaults.celebration,
            fields = TaskFields(
                notes = this[Keys.fieldNotes] ?: true,
                subtasks = this[Keys.fieldSubtasks] ?: true,
                dates = this[Keys.fieldDates] ?: true,
                repeat = this[Keys.fieldRepeat] ?: true,
                priority = this[Keys.fieldPriority] ?: true,
                photos = this[Keys.fieldPhotos] ?: true,
            ),
            reminderOffsetMinutes = this[Keys.reminderOffset] ?: defaults.reminderOffsetMinutes,
            badge = this[Keys.badge] ?: defaults.badge,
            appIcon = AppIconChoice.fromKey(this[Keys.appIcon]) ?: defaults.appIcon,
        )
    }

    private fun MutablePreferences.write(settings: TikSettings) {
        this[Keys.language] = settings.language.tag
        this[Keys.calendar] = settings.calendar.key
        this[Keys.use24Hour] = settings.use24Hour
        this[Keys.theme] = settings.theme.key
        this[Keys.accent] = settings.accent.key
        this[Keys.wallpaperColors] = settings.wallpaperColors
        this[Keys.colorfulBackground] = settings.colorfulBackground
        this[Keys.sortOrder] = settings.sortOrder.key
        this[Keys.showProgress] = settings.showProgress
        this[Keys.showCompletedInToday] = settings.showCompletedInToday
        this[Keys.haptics] = settings.haptics
        this[Keys.sounds] = settings.sounds
        this[Keys.celebration] = settings.celebration
        this[Keys.fieldNotes] = settings.fields.notes
        this[Keys.fieldSubtasks] = settings.fields.subtasks
        this[Keys.fieldDates] = settings.fields.dates
        this[Keys.fieldRepeat] = settings.fields.repeat
        this[Keys.fieldPriority] = settings.fields.priority
        this[Keys.fieldPhotos] = settings.fields.photos
        this[Keys.reminderOffset] = settings.reminderOffsetMinutes
        this[Keys.badge] = settings.badge
        this[Keys.appIcon] = settings.appIcon.key
    }
}
