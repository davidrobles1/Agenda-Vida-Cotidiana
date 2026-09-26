package com.vidacotidiana.sharing.infrastructure;

import com.vidacotidiana.sharing.application.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * El adaptador POR DEFECTO mientras DEC-009 (proveedor de correo) siga sin
 * resolverse — ver el javadoc del puerto. Desde que existe
 * {@link SmtpEmailSender}, este cede el sitio en cuanto se configura
 * `vidacotidiana.mail.enabled=true`: no se elige proveedor, se elige si hay
 * servidor SMTP al que hablar.
 * Logs that an invitation email "would be sent"
 * without sending anything real; no SMTP/API client of any kind. Never logs
 * the recipient's email in full (data minimization, same spirit as
 * shared.api.GlobalExceptionHandler never leaking sensitive details).
 */
@Component
@ConditionalOnMissingBean(SmtpEmailSender.class)
public class NoOpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(NoOpEmailSender.class);

    @Override
    public void sendInvitation(String toEmail, String reminderTitle) {
        log.info("Invitation email would be sent (no-op adapter, DEC-009 pending): recipient masked, reminder='{}'",
                reminderTitle);
    }
}
