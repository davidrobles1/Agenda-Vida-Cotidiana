package com.vidacotidiana.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.vidacotidiana.app.MainActivity
import com.vidacotidiana.app.R
import com.vidacotidiana.app.navigation.DeepLinks
import com.vidacotidiana.app.navigation.Routes

/**
 * AND-007 (ADR-007: "las notificaciones locales ... se mantienen resueltas en el
 * cliente", no red). Own NotificationChannel, separate from whatever channel FCM
 * push (AND-005) uses — so the user can silence one without the other, as asked.
 */
object LocalReminderNotifier {
    const val CHANNEL_ID = "local_reminders"
    // En español, como el resto de la aplicación: el nombre y la descripción del
    // canal se ven en los ajustes del SISTEMA, así que estaban en inglés a la
    // vista del usuario.
    private const val CHANNEL_NAME = "Tareas que vencen"
    private const val CHANNEL_DESCRIPTION =
        "Avisos de tus propias tareas al llegar su hora. No usan red: los resuelve el teléfono."

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

    /**
     * LLEVA A ESA TAREA, no a la lista.
     *
     * Antes el intent era un `MainActivity::class.java` desnudo, sin `data`: al
     * tocar el aviso la aplicación se abría donde el usuario la hubiera dejado y
     * tenía que ir a buscar aquello de lo que se le acababa de avisar. Eso se
     * arregló llevando a la lista de Tareas, con este razonamiento: «no existe
     * una ruta de detalle por tarea, se abren como hoja de edición desde su
     * lista».
     *
     * ESE RAZONAMIENTO CADUCÓ. `Routes.TASK_DETAIL` existe desde que la tarea
     * tiene pantalla propia —con sus pasos, sus adjuntos y su estado—, así que
     * dejar el aviso en la lista obligaba a buscar entre varias la que acababa
     * de avisar. Un aviso que nombra una tarea concreta tiene que abrir esa
     * tarea concreta.
     *
     * Si la tarea ya no existe cuando se toca el aviso —borrada desde otro
     * dispositivo—, su pantalla lo dice y ofrece volver. No hace falta
     * comprobarlo aquí, que además sería comprobarlo con datos viejos.
     */
    fun show(context: Context, reminderId: String, title: String) {
        val contentIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            Intent(
                Intent.ACTION_VIEW,
                DeepLinks.section(Routes.taskRoute(reminderId)).toUri(),
                context,
                MainActivity::class.java,
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle("Te toca")
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
