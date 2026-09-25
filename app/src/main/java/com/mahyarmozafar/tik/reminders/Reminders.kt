package com.mahyarmozafar.tik.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mahyarmozafar.tik.MainActivity
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.app.localized
import com.mahyarmozafar.tik.model.AppLanguage
import com.mahyarmozafar.tik.model.Task
import com.mahyarmozafar.tik.model.TaskList
import com.mahyarmozafar.tik.model.TikSettings
import com.mahyarmozafar.tik.ui.theme.system
import androidx.compose.ui.graphics.toArgb
import java.time.Instant

/**
 * Local notifications for tasks that have a time.
 *
 * Each task has at most one waiting alarm, keyed by the task's id, so it is easy to move or
 * remove when the task changes. When the alarm goes off, [ReminderReceiver] shows the reminder.
 */
class Reminders(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = NotificationManagerCompat.from(context)

    val canNotify: Boolean
        get() = notifications.areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    /** Exact alarms need a special permission on Android 12 and 13 phones that don't have [USE_EXACT_ALARM]. */
    val canScheduleExact: Boolean
        get() = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()

    /** The channels' names follow the app's language. */
    fun createChannels(language: AppLanguage) {
        val texts = context.localized(language)
        notifications.createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(CHANNEL_REMINDERS, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName(texts.getString(R.string.channel_reminders))
                    .setDescription(texts.getString(R.string.channel_reminders_explained))
                    .build(),
                NotificationChannelCompat.Builder(CHANNEL_COUNT, NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName(texts.getString(R.string.channel_count))
                    .setDescription(texts.getString(R.string.channel_count_explained))
                    .setShowBadge(true)
                    .setVibrationEnabled(false)
                    .setSound(null, null)
                    .build(),
            ),
        )
    }

    /** Makes the waiting alarm match the task: adds it, moves it, or removes it. */
    fun sync(task: Task, settings: TikSettings, now: Instant = Instant.now()) {
        alarms.cancel(alarmIntent(task.id))
        val due = task.dueDate ?: return
        if (task.isDone || !task.hasTime) return
        val fireAt = due.minusSeconds(settings.reminderOffsetMinutes * 60L)
        if (!fireAt.isAfter(now)) return
        schedule(task.id, fireAt)
    }

    /** Schedules every open task again, after a setting changed or the phone restarted. */
    fun resyncAll(tasks: List<Task>, settings: TikSettings) {
        val now = Instant.now()
        tasks.forEach { sync(it, settings, now) }
    }

    fun schedule(taskId: String, at: Instant, snoozed: Boolean = false) {
        val intent = alarmIntent(taskId, snoozed)
        if (canScheduleExact) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), intent)
        }
    }

    /** Removes waiting alarms and reminders that are showing. */
    fun cancel(taskIds: List<String>) {
        for (id in taskIds) {
            alarms.cancel(alarmIntent(id))
            alarms.cancel(alarmIntent(id, snoozed = true))
            notifications.cancel(id, NOTIFICATION_ID)
        }
    }

    fun dismiss(taskId: String) = notifications.cancel(taskId, NOTIFICATION_ID)

    /** Shows the reminder now, with "Mark as Done" and "Remind Me in 10 Minutes" buttons. */
    fun show(task: Task, list: TaskList?, settings: TikSettings) {
        if (!canNotify) return
        val texts = context.localized(settings.language)
        val formatting = settings.formatting()

        val parts = buildList {
            task.dueDate?.let { add(formatting.time(it)) }
            list?.let { add(it.name) }
            task.note.lineSequence().firstOrNull { it.isNotBlank() }?.let { add(it.trim()) }
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(settings.accent.system.color(dark = false).toArgb())
            .setContentTitle(task.title)
            .setContentText(parts.joinToString(" · "))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openToday(context))
            .addAction(
                R.drawable.ic_check,
                texts.getString(R.string.mark_as_done),
                actionIntent(ReminderReceiver.ACTION_DONE, task.id),
            )
            .addAction(
                R.drawable.ic_clock,
                texts.getString(R.string.remind_me_in_minutes, SNOOZE_MINUTES.toString()),
                actionIntent(ReminderReceiver.ACTION_SNOOZE, task.id),
            )
            .build()
        try {
            notifications.notify(task.id, NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notifications were turned off in the meantime.
        }
    }

    private fun alarmIntent(taskId: String, snoozed: Boolean = false): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(if (snoozed) ReminderReceiver.ACTION_SNOOZED_FIRE else ReminderReceiver.ACTION_FIRE)
            .setData(taskUri(taskId))
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun actionIntent(action: String, taskId: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(action).setData(taskUri(taskId))
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_COUNT = "today-count"
        const val SNOOZE_MINUTES = 10
        private const val NOTIFICATION_ID = 1

        fun taskUri(taskId: String): Uri = Uri.parse("tik://task/$taskId")

        fun openToday(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_TODAY)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
