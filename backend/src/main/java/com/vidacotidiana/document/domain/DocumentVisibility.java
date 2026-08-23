package com.vidacotidiana.document.domain;

/**
 * PRIVATE: solo el dueño (default). SHARED: dueño + un usuario específico
 * (Document#sharedWithUserId, resuelto por email — ver Document#shareWith).
 * FAMILY_PUBLIC: visible a cualquier otra cuenta de esta instancia —
 * ASSUMPTION explícita (ver V12__documents.sql), no existe un modelo de
 * "grupo familiar" real hoy.
 */
public enum DocumentVisibility {
    PRIVATE,
    SHARED,
    FAMILY_PUBLIC
}
