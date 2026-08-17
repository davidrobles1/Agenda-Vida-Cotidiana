package com.vidacotidiana.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * AND-007. AlarmManager alarms are cleared by the OS on every reboot — without
 * this, a pending local reminder notification would silently never fire again
 * after any restart, leaving the feature only "half" working. Reads the
 * SharedPreferences-backed store (ReminderAlarmScheduler) written at schedule
 * time and re-arms each alarm that's still in the future.
 */
class BootRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        ReminderAlarmScheduler(context).rescheduleAllPersisted()
    }
}
