package com.cadence.daf

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Fires the daily reminder, and re-arms it after it fires, after a reboot and after an app update. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context).settings.first()
                // The alarm intent has no action; boot and update broadcasts only re-arm.
                if (intent.action == null && settings.reminderEnabled) {
                    val today = practiceStats(PracticeLogStore(context).readLines(), LocalDate.now()).todayMin
                    if (shouldRemind(today, settings.dailyGoalMin)) Reminders.notify(context)
                }
                Reminders.schedule(context, settings.reminderEnabled, settings.reminderMinute)
            } finally {
                result.finish()
            }
        }
    }
}
