-- ADR-025 — los tipos de evento de auditoría que introduce Familia/Compartidos.
--
-- MIGRACIÓN APARTE, y no un añadido a V29, por disciplina de Flyway: V29 ya
-- estaba aplicada en el entorno local cuando se detectó que faltaba esto.
-- Editarla habría cambiado su checksum y dejado la base de datos en estado
-- inválido en el próximo arranque; una migración nueva es la única forma
-- correcta de corregir algo ya aplicado.
--
-- QUÉ ROMPÍA: `audit_events` (V4) enumera los tipos permitidos en una CHECK.
-- Al añadir valores al enum de Java sin tocarla, CADA acción de familia o de
-- compartición estallaba con un 500 al intentar registrarse. Lo detectó la
-- prueba de integración de este ADR y se reprodujo en el entorno local.
--
-- Se reemplaza la restricción entera en vez de añadir una segunda: dos CHECK
-- sobre la misma columna se evalúan en AND, así que la vieja seguiría
-- rechazando los valores nuevos.

ALTER TABLE audit_events DROP CONSTRAINT ck_audit_events_event_type;
ALTER TABLE audit_events ADD CONSTRAINT ck_audit_events_event_type CHECK (event_type IN (
    'INVITATION_CREATED', 'INVITATION_CANCELLED', 'INVITATION_ACCEPTED',
    'INVITATION_REJECTED', 'INVITATION_EXPIRED', 'SHARE_REVOKED',
    -- ADR-025
    'FAMILY_INVITATION_CREATED', 'FAMILY_INVITATION_ACCEPTED',
    'FAMILY_INVITATION_REJECTED', 'FAMILY_INVITATION_CANCELLED',
    'FAMILY_LINK_REMOVED',
    'RESOURCE_SHARED', 'RESOURCE_SHARE_REVOKED',
    'RESOURCE_PART_DONE', 'RESOURCE_PART_REOPENED'
));

ALTER TABLE audit_events DROP CONSTRAINT ck_audit_events_target_type;
ALTER TABLE audit_events ADD CONSTRAINT ck_audit_events_target_type CHECK (target_type IN (
    'REMINDER', 'INVITATION', 'REMINDER_SHARE',
    -- ADR-025
    'FAMILY_INVITATION', 'FAMILY_LINK', 'RESOURCE_SHARE'
));
