package com.mahyarmozafar.tik.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.ZoneId

/**
 * Wakes Tik a minute after midnight, so the widget and the count move to the new day even when
 * the app isn't open. It doesn't need to be exact.
 */
object DayChange {
    const val ACTION = "com.mahyarmozafar.tik.NEW_DAY"

    fun schedule(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val zone = ZoneId.systemDefault()
        val nextDay = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).plusMinutes(1)
        val intent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, RefreshReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        alarms.set(AlarmManager.RTC, nextDay.toInstant().toEpochMilli(), intent)
    }
}
