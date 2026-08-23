-- V12__documents.sql
-- Módulo Documentos (pedido explícito del usuario, 2026-08-22): antes
-- "scaffolding-only" (web/src/features/documents/DocumentsPage.tsx, UX-006,
-- mock data sin backend real). Mismo patrón de almacenamiento que
-- V10__vision_board_images.sql (Postgres BYTEA, sin object storage
-- S3-compatible provisionado en este entorno — ver ese archivo para el
-- razonamiento completo, aplica igual aquí) — content_type/size_bytes/data
-- son idénticos en forma.
--
-- category: las 5 categorías pedidas explícitamente por el usuario
-- (Identificación, Comprobantes, Seguros, Contratos, Otros) — CHECK
-- constraint, mismo patrón que warranties.status.
--
-- Compartir (pedido explícito, aclarado por el usuario: "no necesitamos
-- avisar por correo, necesitamos que a quien se comparta visualice de
-- inmediato, no que tenga que revisar el correo"): a diferencia de
-- V2__sharing.sql (Invitation con estado PENDING/ACCEPTED, pensado para
-- alguien que aún no tiene cuenta), este módulo no tiene un flujo de
-- invitación — comparte directo contra un shared_with_user_id ya
-- resuelto, visible de inmediato en el listado del destinatario. No existe
-- hoy un modelo de "familia"/grupo en el backend (ver research de la
-- sesión) — shared_with_email se guarda siempre (lo que el dueño escribió,
-- para mostrarlo en su propia vista) y shared_with_user_id solo si ese
-- correo resolvió a una cuenta real (users.email) — mismo patrón de
-- resolución que SharingService#createInvitation usa para reminders, sin
-- revelar en la respuesta si hubo match o no (SEC-001, no-enumeration).
--
-- visibility PRIVATE|SHARED|FAMILY_PUBLIC: FAMILY_PUBLIC es una
-- ASSUMPTION explícita — no existe un modelo de "grupo familiar" real, así
-- que "público entre la familia" se interpreta como "visible a cualquier
-- otra cuenta de usuario en esta instancia" (esta app está diseñada para
-- una sola familia por instancia, no multi-tenant — ver AI-CONTEXT.md).
-- Documentado explícitamente para poder revisarse si el producto define
-- un modelo de familia real más adelante.

CREATE TABLE documents (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id     UUID NOT NULL REFERENCES users (id),
    name              VARCHAR(200) NOT NULL,
    category          VARCHAR(32) NOT NULL,
    content_type      VARCHAR(64) NOT NULL,
    size_bytes        BIGINT NOT NULL,
    data              BYTEA NOT NULL,
    visibility        VARCHAR(32) NOT NULL DEFAULT 'PRIVATE',
    shared_with_email VARCHAR(255),
    shared_with_user_id UUID REFERENCES users (id),
    version           INTEGER NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_documents_category CHECK (category IN ('IDENTIFICACION', 'COMPROBANTES', 'SEGUROS', 'CONTRATOS', 'OTROS')),
    CONSTRAINT ck_documents_visibility CHECK (visibility IN ('PRIVATE', 'SHARED', 'FAMILY_PUBLIC'))
);

CREATE INDEX ix_documents_owner_user_id ON documents (owner_user_id);
CREATE INDEX ix_documents_shared_with_user_id ON documents (shared_with_user_id);
CREATE INDEX ix_documents_visibility ON documents (visibility);
