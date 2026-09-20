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
     * Si el teléfono nos deja DIBUJAR el aviso.
     *
     * Son dos permisos distintos y hacen falta los dos: uno para que suene la
     * alarma a su hora (`SCHEDULE_EXACT_ALARM`) y otro para que lo que la alarma
     * produce llegue a verse (`POST_NOTIFICATIONS`, obligatorio desde Android
     * 13). `LocalReminderNotifier` ya comprobaba el segundo y no dibujaba nada
     * si faltaba… pero NADIE lo pedía nunca, así que en un teléfono moderno la
     * función entera estaba muerta desde el primer día: la alarma sonaba y el
     * aviso se descartaba en silencio.
     *
     * Se expone desde aquí, y no se comprueba en la pantalla, porque el momento
     * de preguntar es el momento de programar: es cuando el permiso significa
     * algo para el usuario.
     */
    fun avisosPermitidos(): Boolean =
        androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * No-ops (does not throw) if dueAtMillis is already in the past, or if
     * exact-alarm permission isn't granted — the caller (RemindersScreen) is
     * responsible for prompting for that permission before calling this; this
     * function fails closed rather than crashing if it's called anyway.
     */
    fun schedule(reminderId: String, title: String, dueAtMillis: Long) {
        if (dueAtMillis <= System.currentTimeMillis()) return

        /*
         * SIN PERMISO DE ALARMA EXACTA, APROXIMADA — PERO NUNCA NADA.
         *
         * Esto era `if (!canScheduleExactAlarms()) return`: se rendía en
         * silencio. Y en Android 12+ `SCHEDULE_EXACT_ALARM` NO se concede sola
         * a una aplicación con `targetSdk` moderno, así que en la práctica no
         * se programaba ninguna alarma y el aviso de una tarea no llegaba
         * jamás. El comentario original delegaba la petición del permiso en
         * «RemindersScreen», una pantalla que ya no existe.
         *
         * Un aviso que puede retrasarse unos minutos es incomparablemente
         * mejor que uno que no llega. `setAndAllowWhileIdle` atraviesa el modo
         * de ahorro igual que su hermana exacta; lo único que pierde es la
         * puntualidad al minuto, y eso se recupera en cuanto el usuario
         * concede el permiso desde los ajustes del sistema.
         */
        val pendingIntent = pendingIntentFor(reminderId, title)
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAtMillis, pendingIntent)
        }
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
                // Misma regla que `schedule`: exacta si se puede, aproximada
                // si no, pero nunca dejar la tarea sin aviso por un permiso.
                schedule(scheduled.reminderId, scheduled.title, scheduled.dueAtMillis)
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
