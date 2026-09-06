package com.vidacotidiana.audit.domain;

/**
 * 11-auth-security.md §Auditoría — los eventos que enumera, y nada más.
 *
 * Los bloques de Familia y Compartidos los añade ADR-025 junto con la
 * capacidad que auditan: entrar o salir de una familia y ceder acceso a un
 * recurso propio son exactamente el tipo de hecho que esa sección pide
 * registrar. No se inventa un evento sin acción que lo produzca.
 */
public enum AuditEventType {
    INVITATION_CREATED,
    INVITATION_CANCELLED,
    INVITATION_ACCEPTED,
    INVITATION_REJECTED,
    INVITATION_EXPIRED,
    SHARE_REVOKED,

    // ADR-025 — Familia
    FAMILY_INVITATION_CREATED,
    FAMILY_INVITATION_ACCEPTED,
    FAMILY_INVITATION_REJECTED,
    FAMILY_INVITATION_CANCELLED,
    FAMILY_LINK_REMOVED,

    // ADR-025 — Compartidos
    RESOURCE_SHARED,
    RESOURCE_SHARE_REVOKED,
    RESOURCE_PART_DONE,
    RESOURCE_PART_REOPENED
}
