package com.cadence.daf

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.time.LocalDateTime
import java.time.ZoneId

private const val WINDOW_MS = 10 * 60 * 1000L
private const val CHANNEL_ID = "cadence_reminders"
private const val NOTIFICATION_ID = 2

/** Today at [minuteOfDay] if that is still ahead, otherwise tomorrow. */
fun nextReminderAt(now: LocalDateTime, minuteOfDay: Int): LocalDateTime {
    val today = now.toLocalDate().atTime(minuteOfDay / 60, minuteOfDay % 60)
    return if (today.isAfter(now)) today else today.plusDays(1)
}

/** No reminder once today's practice is done: the goal is reached, or any minute when the goal is off. */
fun shouldRemind(todayMin: Int, goalMin: Int): Boolean = if (goalMin == 0) todayMin == 0 else todayMin < goalMin

object Reminders {
    /** Replaces any pending alarm with the next one, or none when the reminder is off. */
    fun schedule(context: Context, enabled: Boolean, minuteOfDay: Int) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(
            context, 0, Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarms.cancel(pending)
        if (!enabled) return
        val at = nextReminderAt(LocalDateTime.now(), minuteOfDay).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // A window alarm needs no exact-alarm permission; up to 10 minutes late is fine for a reminder.
        alarms.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MS, pending)
    }

    fun notify(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT)
        )
        manager.notify(NOTIFICATION_ID, build(context))
    }

    private fun build(context: Context): Notification {
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.brand_teal))
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
    }
}
