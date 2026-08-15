package com.vidacotidiana.notification.infrastructure;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.vidacotidiana.notification.application.PushEvent;
import com.vidacotidiana.notification.application.PushNotificationSender;
import com.vidacotidiana.notification.domain.DevicePushToken;
import com.vidacotidiana.notification.domain.DevicePushTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Real adapter (ADR-007/DEC-010): sends to every registered device of the
 * recipient via Firebase Cloud Messaging. Only instantiated by
 * PushNotificationConfig when firebase.credentials-path is set — not the
 * case in this development environment (no real Firebase project
 * available here) nor in any test profile, so this class's actual delivery
 * path is exercised only structurally in tests (payload construction
 * against a mocked FirebaseMessaging client), never against the real
 * service. See docs/development/02-validation-report.md for that
 * distinction spelled out explicitly.
 */
public class FcmPushNotificationSender implements PushNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(FcmPushNotificationSender.class);

    private final FirebaseMessaging firebaseMessaging;
    private final DevicePushTokenRepository devicePushTokenRepository;

    public FcmPushNotificationSender(FirebaseMessaging firebaseMessaging, DevicePushTokenRepository devicePushTokenRepository) {
        this.firebaseMessaging = firebaseMessaging;
        this.devicePushTokenRepository = devicePushTokenRepository;
    }

    @Override
    public void sendBestEffort(UUID recipientUserId, PushEvent event) {
        try {
            List<DevicePushToken> devices = devicePushTokenRepository.findByUserId(recipientUserId);
            for (DevicePushToken device : devices) {
                Message message = Message.builder()
                        .setToken(device.getToken())
                        .setNotification(Notification.builder().setBody(event.message()).build())
                        .putData("type", event.type().name())
                        .build();
                firebaseMessaging.send(message);
            }
        } catch (FirebaseMessagingException | RuntimeException ex) {
            // AC-012: best-effort — never propagate a push failure into the caller's main operation.
            log.warn("Push notification failed (best-effort, not retried): recipientUserId={}, eventType={}",
                    recipientUserId, event.type(), ex);
        }
    }
}
