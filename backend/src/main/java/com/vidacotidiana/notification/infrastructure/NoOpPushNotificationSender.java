package com.vidacotidiana.notification.infrastructure;

import com.vidacotidiana.notification.application.PushEvent;
import com.vidacotidiana.notification.application.PushNotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Default adapter (see PushNotificationConfig) — active whenever
 * firebase.credentials-path is not configured, which includes this
 * development environment and every test profile. Logs that a push
 * "would be sent" without contacting any real service. Never logs the
 * recipient's user id together with message content beyond what's already
 * safe to log server-side.
 */
public class NoOpPushNotificationSender implements PushNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(NoOpPushNotificationSender.class);

    @Override
    public void sendBestEffort(UUID recipientUserId, PushEvent event) {
        log.info("Push notification would be sent (no-op adapter, no firebase.credentials-path configured): "
                + "recipientUserId={}, eventType={}", recipientUserId, event.type());
    }
}
