package com.vidacotidiana.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * AND-007. Fired by AlarmManager at a reminder's exact dueAt. Not a Hilt
 * @AndroidEntryPoint — this codebase has no existing Hilt-injected receiver to
 * follow as a pattern, and ReminderAlarmScheduler's state lives entirely in
 * SharedPreferences, so constructing a plain instance here is equivalent to
 * the Hilt-provided singleton used from the ViewModel.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val title = intent.getStringExtra(EXTRA_REMINDER_TITLE) ?: return

        LocalReminderNotifier.show(context, reminderId, title)
        // Remove it from the persisted store now that it fired — otherwise a
        // later reboot would see it as still-pending and reschedule something
        // that already happened.
        ReminderAlarmScheduler(context).removeFromStore(reminderId)
    }
}
