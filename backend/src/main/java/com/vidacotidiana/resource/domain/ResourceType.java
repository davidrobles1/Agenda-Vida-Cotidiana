package com.vidacotidiana.resource.domain;

/**
 * ADR-016 Fase 3e4/FR-034. Los seis tipos aprobados, en español porque así
 * los definió el Product Owner y así se muestran (el resto del dominio usa
 * inglés para valores técnicos como PENDING/MINE, pero estos son etiquetas
 * de negocio elegidas explícitamente, no estados internos).
 *
 * DOCUMENTO aquí es una <b>referencia</b> a un documento, no un archivo
 * almacenado: los archivos reales siguen viviendo en DOCUMENT (FR-030).
 */
public enum ResourceType {
    DOCUMENTO,
    ENLACE,
    PLANTILLA,
    MANUAL,
    HERRAMIENTA,
    OTRO
}
