package com.vidacotidiana.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vidacotidiana.app.MainActivity
import com.vidacotidiana.app.R

/**
 * AND-007 (ADR-007: "las notificaciones locales ... se mantienen resueltas en el
 * cliente", no red). Own NotificationChannel, separate from whatever channel FCM
 * push (AND-005) uses — so the user can silence one without the other, as asked.
 */
object LocalReminderNotifier {
    const val CHANNEL_ID = "local_reminders"
    private const val CHANNEL_NAME = "Reminder due"
    private const val CHANNEL_DESCRIPTION = "Local alerts for your own reminders reaching their due time — no network involved."

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = CHANNEL_DESCRIPTION
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(context: Context, reminderId: String, title: String) {
        val contentIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle("Reminder due")
            .setContentText(title)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // POST_NOTIFICATIONS (API 33+) may have been denied — NotificationManagerCompat
        // checks internally and no-ops rather than throwing, but be explicit about why
        // this can silently do nothing instead of assuming it always shows.
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context).notify(reminderId.hashCode(), notification)
        }
    }
}
