package com.vidacotidiana.maintenance.api.dto;

import jakarta.validation.constraints.Size;

/** ADR-021. La nota es opcional: marcar "Hecho" sin escribir nada es el
    caso normal; la nota sirve para "cambié también el filtro". */
public record CompleteOccurrenceRequest(@Size(max = 500) String note) {
}
