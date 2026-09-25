package com.mahyarmozafar.tik.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mahyarmozafar.tik.TikApplication
import com.mahyarmozafar.tik.widget.TodayWidget
import kotlinx.coroutines.launch
import java.time.Instant

/** Shows reminders when their alarm goes off, and runs the buttons on them. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as TikApplication
        val taskId = intent.data?.lastPathSegment ?: return
        val pending = goAsync()
        app.scope.launch {
            try {
                when (intent.action) {
                    ACTION_FIRE, ACTION_SNOOZED_FIRE -> app.showReminder(taskId)
                    ACTION_DONE -> {
                        app.reminders.dismiss(taskId)
                        app.model.markDoneFromReminder(taskId)
                    }
                    ACTION_SNOOZE -> {
                        app.reminders.dismiss(taskId)
                        val later = Instant.now().plusSeconds(Reminders.SNOOZE_MINUTES * 60L)
                        app.reminders.schedule(taskId, later, snoozed = true)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.mahyarmozafar.tik.REMINDER"
        const val ACTION_SNOOZED_FIRE = "com.mahyarmozafar.tik.REMINDER_AGAIN"
        const val ACTION_DONE = "com.mahyarmozafar.tik.DONE"
        const val ACTION_SNOOZE = "com.mahyarmozafar.tik.SNOOZE"
    }
}

/**
 * Alarms are forgotten when the phone restarts or the app is updated, and a new day or a new
 * time zone changes what "today" is. A new phone language can change which way the widget runs.
 * Each of these brings reminders, the widget and the count up to date.
 */
class RefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as TikApplication
        val pending = goAsync()
        app.scope.launch {
            try {
                app.model.refreshAll()
                if (intent.action == Intent.ACTION_LOCALE_CHANGED) TodayWidget.publishPreviews(app)
            } finally {
                pending.finish()
            }
        }
    }
}
