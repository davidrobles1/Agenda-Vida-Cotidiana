package com.vidacotidiana.shared.domain;

/**
 * ADR-019: módulo propietario de un recurso — Personal o Laboral.
 *
 * Pedido explícito del usuario (2026-08-28): "cada recurso debe pertenecer
 * exclusivamente al módulo desde el cual fue creado... el aislamiento debe
 * existir también en la lógica de negocio y en las consultas, no únicamente
 * ocultando elementos mediante filtros visuales".
 *
 * Vive en `shared` y no en un módulo concreto porque lo comparten cuatro
 * agregados sin relación entre sí (Garantía, Mantenimiento, Suscripción,
 * Nota del día). El enum `reminder.domain.ReminderContext` se deja
 * intacto: ya existía, ya está persistido y cambiarlo obligaría a tocar el
 * módulo `reminder` entero sin ninguna ganancia — son dos representaciones
 * del mismo concepto que conviven, y `ReminderService` sigue siendo su
 * único dueño.
 *
 * REGLA DE MIGRACIÓN (V25): todo lo que ya existía es PERSONAL. No se
 * reinterpreta nada como LABORAL.
 */
public enum ModuleContext {
    PERSONAL,
    LABORAL;

    /**
     * Resuelve el contexto recibido de la API. Nulo o vacío ⇒ PERSONAL: es
     * el valor que tenían todos los recursos anteriores a esta regla y el
     * que corresponde a cualquier pantalla que aún no envía contexto.
     */
    public static ModuleContext fromNullable(String raw) {
        if (raw == null || raw.isBlank()) {
            return PERSONAL;
        }
        return ModuleContext.valueOf(raw.trim().toUpperCase());
    }

    /**
     * Filtro de consulta: nulo ⇒ "sin filtrar", que es lo que necesita el
     * Calendario general (el modo que muestra Personal y Laboral juntos).
     * No confundir con {@link #fromNullable}, que resuelve el contexto de
     * ALTA de un recurso y ahí sí hay que decidirse por uno.
     */
    public static ModuleContext filterFromNullable(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return ModuleContext.valueOf(raw.trim().toUpperCase());
    }
}
