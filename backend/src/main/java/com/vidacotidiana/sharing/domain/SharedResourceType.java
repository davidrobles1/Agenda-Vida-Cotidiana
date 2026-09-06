package com.vidacotidiana.sharing.domain;

/**
 * Los recursos que se pueden compartir con la familia (ADR-025 §2).
 *
 * Son los que YA existen como entidad propia con dueño. Dos ausencias
 * deliberadas, ninguna es un olvido:
 *
 * <b>ALERTA</b> — las alertas se DERIVAN, no se almacenan
 * (ADR-018; ver web/src/features/calendar/alerts/dateAlerts.ts). No tienen
 * fila, ni id de base de datos, ni dueño: son una VISTA de una garantía, un
 * mantenimiento o un pago. Compartir el registro de origen comparte su alerta
 * automáticamente, que es justo la propiedad por la que se eligió derivarlas.
 * Crear una tabla de alertas para poder compartirlas desharía esa decisión.
 *
 * <b>EVENTO</b> — no existe una entidad Evento. Lo que el calendario muestra
 * como evento es un {@code Reminder} con fecha (ver ReminderContext y
 * CalendarPage): tarea y evento son la misma fila. Añadir un tipo aquí sería
 * inventar una entidad que el dominio no tiene.
 */
public enum SharedResourceType {
    REMINDER,
    MAINTENANCE,
    SUBSCRIPTION,
    WARRANTY,
    INVENTORY_ITEM,
    DOCUMENT;

    /**
     * Si tiene sentido comprometer a alguien con "hacer su parte" de este
     * recurso (requisito §5: "no agregues estados o acciones que no tengan
     * sentido para un recurso determinado").
     *
     * Un artículo de INVENTARIO es algo que se POSEE, no algo que se hace: no
     * existe "ya hice mi parte" de una silla. Un DOCUMENTO es de solo
     * visualización por requisito explícito (§6). En los dos, compartir
     * significa ver. La base de datos impone lo mismo
     * (ck_resource_shares_responsibility, V29) para que no dependa de que la
     * interfaz se acuerde.
     */
    public boolean supportsResponsibility() {
        return this != INVENTORY_ITEM && this != DOCUMENT;
    }
}
