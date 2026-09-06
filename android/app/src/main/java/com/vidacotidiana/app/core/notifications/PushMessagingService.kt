package com.vidacotidiana.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vidacotidiana.app.MainActivity
import com.vidacotidiana.app.R
import com.vidacotidiana.app.navigation.DeepLinks
import com.vidacotidiana.app.navigation.PushDestinations
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Recibe los push que el backend YA envía.
 *
 * El backend tiene `PushNotificationSender` con su adapter de FCM y lo invoca
 * desde familia, compartición de recordatorios y recursos compartidos. En
 * Android faltaban las dos puntas: nadie registraba el token del dispositivo y
 * nadie recogía los mensajes. Esta clase cierra la segunda;
 * [PushTokenRegistrar] cierra la primera.
 *
 * DOS CAMINOS, y hay que atender los dos. El backend construye el mensaje con
 * `setNotification(...)`, así que:
 *
 *  - Con la aplicación en PRIMER PLANO, FCM entrega aquí y no muestra nada:
 *    la notificación la construimos nosotros, y por eso puede llevar el enlace
 *    a la sección correcta.
 *  - Con la aplicación en SEGUNDO PLANO o cerrada, FCM dibuja la notificación
 *    él mismo y este método NO se llama. Al tocarla, el sistema abre la
 *    actividad de lanzamiento con los datos del mensaje en los extras del
 *    intent — y de ahí los recoge `MainActivity`.
 *
 * Tratar solo el primer caso habría dejado sin destino justo la situación más
 * común, que es recibir el aviso con la aplicación cerrada.
 */
@AndroidEntryPoint
class PushMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var tokenRegistrar: PushTokenRegistrar

    override fun onNewToken(token: String) {
        // FCM rota el token por su cuenta (reinstalación, restauración, purga).
        // Si no se reenviara, el backend seguiría escribiendo a un token muerto
        // y los avisos dejarían de llegar sin que nada fallara visiblemente.
        tokenRegistrar.onTokenRefreshed(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"]
        val body = message.notification?.body ?: message.data["message"] ?: return

        createChannel(this)

        val deepLink = DeepLinks.section(PushDestinations.routeFor(type)).toUri()
        val contentIntent = PendingIntent.getActivity(
            this,
            // Un id por tipo de evento: dos avisos distintos no deben pisarse
            // el `PendingIntent`, y dos del mismo tipo sí pueden reutilizarlo.
            (type ?: "vc").hashCode(),
            Intent(Intent.ACTION_VIEW, deepLink, this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(CHANNEL_NAME)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // POST_NOTIFICATIONS puede estar denegado: se comprueba en vez de
        // asumir que siempre se muestra (mismo criterio que el aviso local).
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            NotificationManagerCompat.from(this).notify(body.hashCode(), notification)
        }
    }

    companion object {
        /**
         * Canal PROPIO, separado del de recordatorios locales: uno lo dispara
         * el reloj del teléfono y el otro lo dispara otra persona. Separarlos
         * permite al usuario silenciar uno sin perder el otro, que es
         * exactamente para lo que existen los canales.
         */
        const val CHANNEL_ID = "family_and_sharing"
        private const val CHANNEL_NAME = "Familia y compartidos"
        private const val CHANNEL_DESCRIPTION =
            "Avisos de invitaciones y de recursos que alguien comparte contigo."

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = CHANNEL_DESCRIPTION }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
