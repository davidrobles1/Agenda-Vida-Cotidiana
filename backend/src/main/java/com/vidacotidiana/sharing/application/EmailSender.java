package com.vidacotidiana.sharing.application;

/**
 * Port for sending invitation emails (FR-007), abstracted from any concrete
 * provider — same pattern as PushNotificationSender (ADR-007) for push.
 *
 * DEC-009 (proveedor de correo) was reopened on 2026-08-15: it depended on
 * DEC-008 (AWS → Amazon SES), which was reverted in favor of a self-hosted
 * server (ADR-014). No provider was chosen to replace it without explicit
 * Product Owner instruction. The only adapter today is a no-op/log-only
 * implementation (sharing.infrastructure.NoOpEmailSender); a real adapter
 * plugs in behind this same interface once DEC-009 is resolved, without
 * changing sharing.application.SharingService.
 */
public interface EmailSender {

    void sendInvitation(String toEmail, String reminderTitle);
}
