package com.vidacotidiana.project.domain;

/**
 * El papel de una persona en un proyecto (V32).
 *
 * CLIENTE NO ESTÁ, y es deliberado. El cliente sigue siendo
 * {@code projects.client_person_id} (DECISION del Product Owner, 2026-09-06:
 * cliente y participantes conviven). Si CLIENTE fuera también un rol de esta
 * lista habría dos sitios donde decir lo mismo y acabarían discrepando: se
 * podría marcar a Ana como cliente en el proyecto y a Luis como participante
 * CLIENTE, y ninguna pantalla sabría cuál manda.
 *
 * Los cuatro valores son los del {@code CHECK} de la migración; cambiarlos
 * aquí sin cambiarlo allí rompe el alta en tiempo de ejecución.
 */
public enum ParticipantRole {
    PROVEEDOR,
    CONTACTO,
    COLABORADOR,
    OTRO,
}
