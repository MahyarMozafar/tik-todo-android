package com.mahyarmozafar.tik.reminders

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.app.localized
import com.mahyarmozafar.tik.model.TikSettings

/**
 * "Count on App Icon". Android has no way to put a number on an icon directly, so this shows a
 * quiet notification with the number of tasks left today. Phones that show numbers on icons
 * (Samsung does) show it there.
 */
class TodayCount(private val context: Context, private val reminders: Reminders) {
    private val notifications = NotificationManagerCompat.from(context)

    fun update(openToday: Int, settings: TikSettings) {
        if (!settings.badge || openToday == 0 || !reminders.canNotify) {
            notifications.cancel(NOTIFICATION_ID)
            return
        }
        val texts = context.localized(settings.language)
        val title = texts.resources.getQuantityString(R.plurals.tasks_left_today, openToday, openToday.toString())
        val notification = NotificationCompat.Builder(context, Reminders.CHANNEL_COUNT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setNumber(openToday)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(Reminders.openToday(context))
            .build()
        try {
            notifications.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notifications were turned off in the meantime.
        }
    }

    private companion object {
        const val NOTIFICATION_ID = 7
    }
}
