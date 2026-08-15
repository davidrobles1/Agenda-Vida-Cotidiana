package com.vidacotidiana.sharing.infrastructure;

import com.vidacotidiana.sharing.application.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Only EmailSender adapter until DEC-009 (proveedor de correo) is resolved
 * — see the port's javadoc. Logs that an invitation email "would be sent"
 * without sending anything real; no SMTP/API client of any kind. Never logs
 * the recipient's email in full (data minimization, same spirit as
 * shared.api.GlobalExceptionHandler never leaking sensitive details).
 */
@Component
public class NoOpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(NoOpEmailSender.class);

    @Override
    public void sendInvitation(String toEmail, String reminderTitle) {
        log.info("Invitation email would be sent (no-op adapter, DEC-009 pending): recipient masked, reminder='{}'",
                reminderTitle);
    }
}
