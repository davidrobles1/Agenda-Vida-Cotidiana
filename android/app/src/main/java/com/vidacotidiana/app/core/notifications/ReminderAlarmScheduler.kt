package com.vidacotidiana.app.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ScheduledLocalReminder(val reminderId: String, val title: String, val dueAtMillis: Long)

const val EXTRA_REMINDER_ID = "reminder_id"
const val EXTRA_REMINDER_TITLE = "reminder_title"

/**
 * AND-007. Wraps AlarmManager.setExactAndAllowWhileIdle — a reminder is a
 * specific moment the user chose, not an approximate window, so WorkManager's
 * flex windows (as used for anything best-effort) aren't the right tool here.
 *
 * Persists what's scheduled to SharedPreferences (not just in AlarmManager)
 * because AlarmManager alarms do NOT survive a device reboot — without this,
 * BootRescheduleReceiver would have nothing to reschedule from, and the
 * feature would silently stop working after every restart. A local key-value
 * store is enough for this (a handful of reminders per user); introducing
 * Room for one small list would be over-architecture for what this needs.
 *
 * Not a @Singleton by necessity — the persisted state lives in SharedPreferences
 * on disk, not in memory, so a plain instance constructed directly (e.g. from a
 * BroadcastReceiver, which isn't a Hilt entry point in this codebase) behaves
 * identically to the Hilt-provided one used from the ViewModel.
 */
@Singleton
class ReminderAlarmScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Pre-S, exact alarms need no special permission at all. */
    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    /**
     * No-ops (does not throw) if dueAtMillis is already in the past, or if
     * exact-alarm permission isn't granted — the caller (RemindersScreen) is
     * responsible for prompting for that permission before calling this; this
     * function fails closed rather than crashing if it's called anyway.
     */
    fun schedule(reminderId: String, title: String, dueAtMillis: Long) {
        if (dueAtMillis <= System.currentTimeMillis()) return
        if (!canScheduleExactAlarms()) return

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            dueAtMillis,
            pendingIntentFor(reminderId, title),
        )
        persist(ScheduledLocalReminder(reminderId, title, dueAtMillis))
    }

    /** Called when a reminder is completed — nothing to notify about anymore. */
    fun cancel(reminderId: String) {
        alarmManager.cancel(pendingIntentFor(reminderId, title = ""))
        removeFromStore(reminderId)
    }

    /** Called only by BootRescheduleReceiver after BOOT_COMPLETED. */
    fun rescheduleAllPersisted() {
        val now = System.currentTimeMillis()
        readStore().forEach { scheduled ->
            if (scheduled.dueAtMillis > now) {
                if (canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        scheduled.dueAtMillis,
                        pendingIntentFor(scheduled.reminderId, scheduled.title),
                    )
                }
                // If permission was revoked while the device was off, this
                // reminder silently won't fire — same fail-closed behavior as
                // schedule(). The user would need to reopen the app to see
                // the permission prompt again (RemindersScreen's own check).
            } else {
                // The due moment already passed while the device was off —
                // deliberately dropped rather than fired late with no
                // indication of how overdue it is (see ReminderAlarmReceiver's
                // own note on the analogous case).
                removeFromStore(scheduled.reminderId)
            }
        }
    }

    /** Used by ReminderAlarmReceiver once a scheduled reminder actually fires. */
    fun removeFromStore(reminderId: String) {
        val remaining = readStore().filterNot { it.reminderId == reminderId }
        prefs.edit().putString(KEY_SCHEDULED, Json.encodeToString(remaining)).apply()
    }

    private fun persist(scheduled: ScheduledLocalReminder) {
        val updated = readStore().filterNot { it.reminderId == scheduled.reminderId } + scheduled
        prefs.edit().putString(KEY_SCHEDULED, Json.encodeToString(updated)).apply()
    }

    private fun readStore(): List<ScheduledLocalReminder> {
        val raw = prefs.getString(KEY_SCHEDULED, null) ?: return emptyList()
        return runCatching { Json.decodeFromString<List<ScheduledLocalReminder>>(raw) }.getOrDefault(emptyList())
    }

    private fun pendingIntentFor(reminderId: String, title: String): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_REMINDER_TITLE, title)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val PREFS_NAME = "local_reminder_schedule"
        private const val KEY_SCHEDULED = "scheduled"
    }
}
