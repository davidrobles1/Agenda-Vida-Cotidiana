package com.vidacotidiana.notification.application;

import java.util.UUID;

/**
 * Port for push notifications (ADR-007/DEC-010), same pattern as
 * sharing.application.EmailSender. Two adapters
 * (notification.infrastructure): NoOpPushNotificationSender (default,
 * log-only) and FcmPushNotificationSender (real Firebase Admin SDK,
 * active only when firebase.credentials-path is configured — not set in
 * this development environment or in tests).
 *
 * AC-012: "el fallo del proveedor de push no debe romper la operación
 * principal, solo la notificación (best-effort)". That guarantee lives in
 * the contract of this method, not at each call site: implementations
 * MUST catch and log any failure internally and never throw. Callers
 * (sharing.application.SharingService, reminder.application.ReminderService)
 * call this exactly like a fire-and-forget statement, with no try/catch of
 * their own.
 */
public interface PushNotificationSender {

    void sendBestEffort(UUID recipientUserId, PushEvent event);
}
