package com.vidacotidiana.sharing.infrastructure;

import com.vidacotidiana.sharing.application.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * El adaptador de correo REAL, por SMTP.
 *
 * DEC-009 —qué proveedor de correo— SIGUE SIN DECIDIR, y esta clase no la
 * decide: SMTP es el denominador común de todos ellos (un servidor propio,
 * un relay, cualquier servicio), así que el adaptador queda hecho sin elegir
 * a nadie. El día que se decida, se apunta `spring.mail.*` a su servidor y
 * ya está funcionando.
 *
 * SOLO SE ACTIVA con `vidacotidiana.mail.enabled=true`. Sin esa propiedad
 * manda `NoOpEmailSender`, que es el comportamiento de hoy: un entorno de
 * desarrollo sin servidor de correo no empieza de pronto a fallar al invitar.
 *
 * NUNCA ROMPE LA INVITACIÓN. Igual que el push (ver
 * `PushNotificationSender`), el correo es best-effort: si el servidor no
 * contesta, la invitación ya está creada y el invitado puede aceptarla desde
 * la aplicación. Un fallo aquí se registra y se traga.
 */
@Component
@ConditionalOnProperty(name = "vidacotidiana.mail.enabled", havingValue = "true")
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String appUrl;

    public SmtpEmailSender(
            JavaMailSender mailSender,
            @Value("${vidacotidiana.mail.from:no-reply@vidacotidiana.app}") String from,
            @Value("${vidacotidiana.mail.app-url:}") String appUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.appUrl = appUrl;
    }

    @Override
    public void sendInvitation(String toEmail, String reminderTitle) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject("Te han compartido algo en Vida Cotidiana");
            message.setText(buildBody(reminderTitle));
            mailSender.send(message);
            // El destinatario NO se registra: minimización de datos, el mismo
            // criterio que ya sigue el adaptador que no envía.
            log.info("Invitation email sent for reminder='{}'", reminderTitle);
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo de invitación (reminder='{}'): {}", reminderTitle, e.toString());
        }
    }

    private String buildBody(String reminderTitle) {
        StringBuilder body = new StringBuilder()
                .append("Hola:\n\n")
                .append("Te han invitado a colaborar en «").append(reminderTitle).append("».\n\n");
        if (!appUrl.isBlank()) {
            body.append("Entra en ").append(appUrl).append(" para aceptarla.\n\n");
        } else {
            body.append("Abre Vida Cotidiana para aceptarla.\n\n");
        }
        return body.append("Si no esperabas este correo, puedes ignorarlo.\n").toString();
    }
}
