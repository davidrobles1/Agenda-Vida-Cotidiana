package com.vidacotidiana.reminder.domain;

/**
 * Cuánto aprieta una tarea. Artefacto maestro, pantalla Crear: «¿Cuánto
 * aprieta?» con tres valores, y propiedad visible en el detalle.
 *
 * TRES Y NO CINCO. Una escala de cinco obliga a decidir entre «alta» y «muy
 * alta», que es una distinción que nadie sostiene al cabo de un mes: acaban
 * todas en el mismo peldaño y la escala deja de informar.
 *
 * El valor por defecto es NORMAL, igual que el DEFAULT de la columna en V33:
 * una tarea creada sin decir nada no es urgente ni aplazable, simplemente es.
 */
public enum ReminderPriority {
    LOW,
    NORMAL,
    URGENT;

    /** Tolerante con la entrada: lo que no se reconoce es NORMAL, no un error. */
    public static ReminderPriority from(String raw) {
        if (raw == null || raw.isBlank()) {
            return NORMAL;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return NORMAL;
        }
    }
}
