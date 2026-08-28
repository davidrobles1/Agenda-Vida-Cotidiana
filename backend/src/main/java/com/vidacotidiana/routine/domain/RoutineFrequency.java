package com.vidacotidiana.routine.domain;

/**
 * ADR-016 Fase 3e2/FR-032. Las tres únicas frecuencias aprobadas — no se
 * añaden recurrencias avanzadas ("cada 2 semanas", días específicos del mes),
 * explícitamente fuera de alcance.
 */
public enum RoutineFrequency {
    DAILY,
    WEEKLY,
    MONTHLY
}
