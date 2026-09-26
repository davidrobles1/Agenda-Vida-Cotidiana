package com.vidacotidiana.notification.application;

/**
 * UC-11/AC-012 — los eventos de compartición, y los tres de vencimiento.
 *
 * LOS TRES ÚLTIMOS se añaden porque los avisos de pagos, garantías y
 * mantenimientos solo se veían CON LA APLICACIÓN ABIERTA: los calculaba el
 * cliente al cargar la pantalla, así que un vencimiento con el teléfono en el
 * bolsillo no avisaba de nada. Los emite `DueDateNotifier`, por reloj.
 */
public enum PushEventType {
    INVITATION_RECEIVED,
    INVITATION_ACCEPTED,
    INVITATION_REJECTED,
    INVITATION_CANCELLED,
    REMINDER_SHARE_REVOKED,
    REMINDER_DELETED,
    PAYMENT_DUE,
    WARRANTY_EXPIRING,
    MAINTENANCE_DUE
}
