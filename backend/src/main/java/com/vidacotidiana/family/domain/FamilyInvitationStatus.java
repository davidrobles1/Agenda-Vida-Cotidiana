package com.vidacotidiana.family.domain;

/**
 * ADR-025. Cuatro estados y no más.
 *
 * No hay EXPIRED, a diferencia de {@link com.vidacotidiana.sharing.domain.InvitationStatus}:
 * allí la caducidad a 7 días es una ASSUMPTION registrada para una invitación
 * que cuelga de un recordatorio concreto. Una invitación familiar no cuelga de
 * ningún recurso y no existe decisión de producto que le fije caducidad;
 * inventarle una sería inventar un requisito. Se cancela o se rechaza.
 */
public enum FamilyInvitationStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED
}
