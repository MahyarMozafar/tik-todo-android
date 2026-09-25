package com.mahyarmozafar.tik.ui.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mahyarmozafar.tik.BuildConfig
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.TikApplication
import com.mahyarmozafar.tik.model.AccentChoice
import com.mahyarmozafar.tik.model.AppIconChoice
import com.mahyarmozafar.tik.model.AppLanguage
import com.mahyarmozafar.tik.model.AppTheme
import com.mahyarmozafar.tik.model.CalendarKind
import com.mahyarmozafar.tik.model.TaskFields
import com.mahyarmozafar.tik.model.TaskSortOrder
import com.mahyarmozafar.tik.model.TikSettings
import com.mahyarmozafar.tik.ui.LocalChangeLanguage
import com.mahyarmozafar.tik.ui.LocalModel
import com.mahyarmozafar.tik.ui.components.GlassMenu
import com.mahyarmozafar.tik.ui.components.GroupPosition
import com.mahyarmozafar.tik.ui.components.GroupRow
import com.mahyarmozafar.tik.ui.components.MenuItem
import com.mahyarmozafar.tik.ui.components.SectionFooter
import com.mahyarmozafar.tik.ui.components.SectionTitle
import com.mahyarmozafar.tik.ui.components.SheetScaffold
import com.mahyarmozafar.tik.ui.components.groupPosition
import com.mahyarmozafar.tik.ui.components.softGradient
import com.mahyarmozafar.tik.ui.components.title
import com.mahyarmozafar.tik.ui.glass.card
import com.mahyarmozafar.tik.ui.lists.ChoiceGrid
import com.mahyarmozafar.tik.ui.lists.ColorDot
import com.mahyarmozafar.tik.ui.navigation.Route
import com.mahyarmozafar.tik.ui.navigation.navigator
import com.mahyarmozafar.tik.ui.theme.SystemColor
import com.mahyarmozafar.tik.ui.theme.TikTheme
import com.mahyarmozafar.tik.ui.theme.dynamicColorsAvailable
import com.mahyarmozafar.tik.ui.theme.system

/** Everything that can be changed: language and calendar, the look, the feel, task details and reminders. */
@Composable
fun SettingsScreen() {
    val model = LocalModel.current
    val navigator = navigator
    val settings = TikTheme.settings
    val changeLanguage = LocalChangeLanguage.current
    val dark = TikTheme.colors.dark

    fun change(transform: (TikSettings) -> TikSettings) = model.launch { updateSettings(transform) }

    SheetScaffold(title = stringResource(R.string.settings), onClose = { navigator.close(Route.Settings) }) {
        // Language & Calendar
        SectionTitle(stringResource(R.string.language_and_calendar))
        Group(3) { position ->
            PickerRow(
                position(0), R.drawable.ic_language_fill, SystemColor.Blue.color(dark), stringResource(R.string.language),
                options = AppLanguage.entries, selected = settings.language, label = { it.nativeName },
                tag = "languagePicker",
            ) { if (it != settings.language) changeLanguage(it) }
            PickerRow(
                position(1), R.drawable.ic_calendar_fill, SystemColor.Red.color(dark), stringResource(R.string.calendar),
                options = CalendarKind.entries, selected = settings.calendar,
                label = { stringResource(if (it == CalendarKind.Persian) R.string.shamsi else R.string.gregorian) },
            ) { value -> change { it.copy(calendar = value) } }
            SwitchRow(position(2), R.drawable.ic_clock_fill, SystemColor.Orange.color(dark), stringResource(R.string.use_24_hour), settings.use24Hour) { on ->
                change { it.copy(use24Hour = on) }
            }
        }

        // Appearance
        SectionTitle(stringResource(R.string.appearance))
        Group(if (dynamicColorsAvailable) 5 else 4) { position ->
            var row = 0
            PickerRow(
                position(row++), R.drawable.ic_contrast_fill, SystemColor.Indigo.color(dark), stringResource(R.string.theme),
                options = AppTheme.entries, selected = settings.theme,
                label = {
                    stringResource(
                        when (it) {
                            AppTheme.System -> R.string.theme_system
                            AppTheme.Light -> R.string.theme_light
                            AppTheme.Dark -> R.string.theme_dark
                        },
                    )
                },
            ) { value -> change { it.copy(theme = value) } }
            if (dynamicColorsAvailable) {
                SwitchRow(position(row++), R.drawable.ic_wallpaper_fill, SystemColor.Teal.color(dark), stringResource(R.string.from_wallpaper), settings.wallpaperColors) { on ->
                    change { it.copy(wallpaperColors = on) }
                }
            }
            GroupRow(position(row++)) {
                Column(Modifier.weight(1f).alpha(if (settings.wallpaperColors && dynamicColorsAvailable) 0.4f else 1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsLabel(R.drawable.ic_palette_fill, TikTheme.colors.accent, stringResource(R.string.accent_color))
                    }
                    Spacer(Modifier.height(12.dp))
                    ChoiceGrid(AccentChoice.entries) { choice ->
                        ColorDot(
                            choice.system.color(dark),
                            selected = choice == settings.accent && !(settings.wallpaperColors && dynamicColorsAvailable),
                            label = stringResource(choice.title),
                        ) { change { it.copy(accent = choice, wallpaperColors = false) } }
                    }
                }
            }
            SwitchRow(position(row++), R.drawable.ic_gradient_fill, SystemColor.Pink.color(dark), stringResource(R.string.colorful_background), settings.colorfulBackground) { on ->
                change { it.copy(colorfulBackground = on) }
            }
            GroupRow(position(row)) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsLabel(R.drawable.ic_apps_fill, SystemColor.Blue.color(dark), stringResource(R.string.app_icon))
                    }
                    Spacer(Modifier.height(12.dp))
                    ChoiceGrid(AppIconChoice.entries, columns = 3) { icon ->
                        AppIconOption(icon, selected = icon == settings.appIcon) { change { it.copy(appIcon = icon) } }
                    }
                }
            }
        }
        SectionFooter(stringResource(R.string.app_icon_explained))

        // Today
        SectionTitle(stringResource(R.string.today))
        Group(3) { position ->
            PickerRow(
                position(0), R.drawable.ic_sort_fill, SystemColor.Teal.color(dark), stringResource(R.string.sort_tasks_by),
                options = TaskSortOrder.entries, selected = settings.sortOrder,
                label = {
                    stringResource(
                        when (it) {
                            TaskSortOrder.Time -> R.string.sort_time
                            TaskSortOrder.Priority -> R.string.sort_priority
                            TaskSortOrder.Newest -> R.string.sort_newest
                        },
                    )
                },
            ) { value -> change { it.copy(sortOrder = value) } }
            SwitchRow(position(1), R.drawable.ic_chart_fill, SystemColor.Green.color(dark), stringResource(R.string.show_progress), settings.showProgress) { on ->
                change { it.copy(showProgress = on) }
            }
            SwitchRow(position(2), R.drawable.ic_done_all_fill, SystemColor.Mint.color(dark), stringResource(R.string.keep_done_until_tomorrow), settings.showCompletedInToday) { on ->
                change { it.copy(showCompletedInToday = on) }
            }
        }

        // Feel
        SectionTitle(stringResource(R.string.feel))
        Group(3) { position ->
            SwitchRow(position(0), R.drawable.ic_vibration_fill, SystemColor.Purple.color(dark), stringResource(R.string.haptics), settings.haptics) { on ->
                change { it.copy(haptics = on) }
            }
            SwitchRow(position(1), R.drawable.ic_volume_fill, SystemColor.Pink.color(dark), stringResource(R.string.sounds), settings.sounds) { on ->
                change { it.copy(sounds = on) }
            }
            SwitchRow(position(2), R.drawable.ic_celebration_fill, SystemColor.Orange.color(dark), stringResource(R.string.confetti_when_all_done), settings.celebration) { on ->
                change { it.copy(celebration = on) }
            }
        }

        // Task details
        SectionTitle(stringResource(R.string.task_details))
        val fields = settings.fields
        fun changeFields(transform: (TaskFields) -> TaskFields) = change { it.copy(fields = transform(it.fields)) }
        Group(6) { position ->
            SwitchRow(position(0), R.drawable.ic_notes_fill, SystemColor.Gray.color(dark), stringResource(R.string.notes), fields.notes) { on ->
                changeFields { it.copy(notes = on) }
            }
            SwitchRow(position(1), R.drawable.ic_checklist_fill, SystemColor.Blue.color(dark), stringResource(R.string.subtasks), fields.subtasks) { on ->
                changeFields { it.copy(subtasks = on) }
            }
            SwitchRow(position(2), R.drawable.ic_event_fill, SystemColor.Red.color(dark), stringResource(R.string.date_and_time), fields.dates) { on ->
                changeFields { it.copy(dates = on) }
            }
            SwitchRow(position(3), R.drawable.ic_repeat_fill, SystemColor.Green.color(dark), stringResource(R.string.repeat), fields.repeat, enabled = fields.dates) { on ->
                changeFields { it.copy(repeat = on) }
            }
            SwitchRow(position(4), R.drawable.ic_priority_fill, SystemColor.Orange.color(dark), stringResource(R.string.priority), fields.priority) { on ->
                changeFields { it.copy(priority = on) }
            }
            SwitchRow(position(5), R.drawable.ic_photo_fill, SystemColor.Cyan.color(dark), stringResource(R.string.photos), fields.photos) { on ->
                changeFields { it.copy(photos = on) }
            }
        }
        SectionFooter(stringResource(R.string.task_details_explained))

        // Reminders
        RemindersSection(settings, ::change)

        // About
        Spacer(Modifier.height(22.dp))
        Group(1) { position ->
            GroupRow(position(0)) {
                SettingsLabel(R.drawable.ic_info, SystemColor.Gray.color(dark), stringResource(R.string.version))
                Text(BuildConfig.VERSION_NAME, style = TikTheme.type.body, color = TikTheme.colors.secondaryText)
            }
        }
        SectionFooter(stringResource(R.string.made_by))
    }
}

@Composable
private fun RemindersSection(settings: TikSettings, change: (transform: (TikSettings) -> TikSettings) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as TikApplication
    val dark = TikTheme.colors.dark
    // Checked again when coming back from Android's settings.
    var canNotify by remember { mutableStateOf(app.reminders.canNotify) }
    var canExact by remember { mutableStateOf(app.reminders.canScheduleExact) }
    LifecycleResumeEffect(Unit) {
        canNotify = app.reminders.canNotify
        canExact = app.reminders.canScheduleExact
        onPauseOrDispose {}
    }

    SectionTitle(stringResource(R.string.reminders))
    val rows = 2 + (if (!canNotify) 1 else 0) + (if (!canExact) 1 else 0)
    Group(rows) { position ->
        var row = 0
        val offsets = listOf(0, 5, 15, 30, 60)
        PickerRow(
            position(row++), R.drawable.ic_bell_fill, SystemColor.Red.color(dark), stringResource(R.string.remind_me),
            options = offsets, selected = settings.reminderOffsetMinutes,
            label = {
                stringResource(
                    when (it) {
                        5 -> R.string.minutes_before_5
                        15 -> R.string.minutes_before_15
                        30 -> R.string.minutes_before_30
                        60 -> R.string.hour_before_1
                        else -> R.string.at_the_time
                    },
                )
            },
        ) { value -> change { it.copy(reminderOffsetMinutes = value) } }
        SwitchRow(position(row++), R.drawable.ic_badge_fill, SystemColor.Red.color(dark), stringResource(R.string.count_on_app_icon), settings.badge) { on ->
            change { it.copy(badge = on) }
        }
        if (!canNotify) {
            GroupRow(position(row++), onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }) {
                Icon(painterResource(R.drawable.ic_warning), contentDescription = null, tint = TikTheme.colors.warning, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.turn_on_notifications), style = TikTheme.type.body, color = TikTheme.colors.warning, modifier = Modifier.weight(1f))
            }
        }
        if (!canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            GroupRow(position(row), onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }) {
                Icon(painterResource(R.drawable.ic_warning), contentDescription = null, tint = TikTheme.colors.warning, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.allow_exact_alarms), style = TikTheme.type.body, color = TikTheme.colors.warning, modifier = Modifier.weight(1f))
            }
        }
    }
    SectionFooter(stringResource(R.string.reminders_explained) + " " + stringResource(R.string.count_on_app_icon_explained))
}

/** A group of rows. [content] gets each row's place in the group, to round the right corners. */
@Composable
private fun Group(count: Int, content: @Composable ((Int) -> GroupPosition) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        content { index -> groupPosition(index, count) }
    }
}

/** A settings title with a small colored icon, like the iOS Settings app. */
@Composable
private fun RowScope.SettingsLabel(@DrawableRes icon: Int, color: Color, title: String) {
    Box(
        Modifier.size(30.dp).background(color.softGradient(), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
    Spacer(Modifier.width(12.dp))
    Text(title, style = TikTheme.type.body, color = TikTheme.colors.primaryText, modifier = Modifier.weight(1f))
}

@Composable
private fun SwitchRow(
    position: GroupPosition,
    @DrawableRes icon: Int,
    color: Color,
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    GroupRow(position, onClick = if (enabled) ({ onChange(!checked) }) else null, modifier = Modifier.alpha(if (enabled) 1f else 0.4f)) {
        SettingsLabel(icon, color, title)
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

/** A row that shows its value and opens a menu of choices. */
@Composable
private fun <T> PickerRow(
    position: GroupPosition,
    @DrawableRes icon: Int,
    color: Color,
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    tag: String? = null,
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        GroupRow(position, onClick = { open = true }, modifier = if (tag != null) Modifier.testTag(tag) else Modifier) {
            SettingsLabel(icon, color, title)
            Text(
                label(selected),
                style = TikTheme.type.body,
                color = TikTheme.colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(painterResource(R.drawable.ic_expand), contentDescription = null, tint = TikTheme.colors.tertiaryText, modifier = Modifier.size(20.dp))
        }
        Box(Modifier.align(Alignment.BottomEnd)) {
            GlassMenu(expanded = open, onDismiss = { open = false }) {
                options.forEach { option ->
                    MenuItem(label(option), icon = if (option == selected) R.drawable.ic_check else null) {
                        open = false
                        onSelect(option)
                    }
                }
            }
        }
    }
}

@DrawableRes
private fun AppIconChoice.background(): Int = when (this) {
    AppIconChoice.Blue -> R.drawable.ic_launcher_blue_background
    AppIconChoice.Midnight -> R.drawable.ic_launcher_midnight_background
    AppIconChoice.Light -> R.drawable.ic_launcher_light_background
    AppIconChoice.Mint -> R.drawable.ic_launcher_mint_background
    AppIconChoice.Purple -> R.drawable.ic_launcher_purple_background
    AppIconChoice.Sunset -> R.drawable.ic_launcher_sunset_background
}

@DrawableRes
private fun AppIconChoice.foreground(): Int = when (this) {
    AppIconChoice.Blue -> R.drawable.ic_launcher_blue_foreground
    AppIconChoice.Midnight -> R.drawable.ic_launcher_midnight_foreground
    AppIconChoice.Light -> R.drawable.ic_launcher_light_foreground
    AppIconChoice.Mint -> R.drawable.ic_launcher_mint_foreground
    AppIconChoice.Purple -> R.drawable.ic_launcher_purple_foreground
    AppIconChoice.Sunset -> R.drawable.ic_launcher_sunset_foreground
}

/** One app icon to choose, drawn from its two layers the way a launcher shows it. */
@Composable
private fun AppIconOption(icon: AppIconChoice, selected: Boolean, onClick: () -> Unit) {
    val colors = TikTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(if (selected) Modifier.card(RoundedCornerShape(20.dp)) else Modifier)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(shape)
                .border(0.5.dp, colors.primaryText.copy(alpha = 0.08f), shape),
            contentAlignment = Alignment.Center,
        ) {
            // The layers are 108 dp, of which the middle 72 dp show; 64 dp here is those 72.
            Image(painterResource(icon.background()), contentDescription = null, modifier = Modifier.requiredSize(96.dp))
            Image(painterResource(icon.foreground()), contentDescription = null, modifier = Modifier.requiredSize(96.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(painterResource(R.drawable.ic_completed_fill), contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                stringResource(icon.title),
                style = TikTheme.type.subheadline.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                color = colors.primaryText,
            )
        }
    }
}
