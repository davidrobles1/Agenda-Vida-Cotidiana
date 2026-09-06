-- ADR-025: Familia y Compartidos — grupo familiar real y compartición
-- genérica sobre los recursos que YA existen.
--
-- QUÉ HABÍA ANTES (inspeccionado, no supuesto):
--   · `invitations` + `reminder_shares` (V2) comparten UN recordatorio con
--     UNA persona, resuelta por correo. No hay grupo familiar: la sección
--     Familia del portal era andamiaje con datos simulados
--     (web/src/features/family/FamilyPage.tsx, "mock data, zero endpoints").
--   · `documents.visibility = FAMILY_PUBLIC` declara en su propio comentario
--     que es una ASSUMPTION porque "no existe un modelo de grupo familiar
--     real hoy". Esta migración crea ese modelo; la columna se deja INTACTA
--     (no se migra ni se reinterpreta ningún documento existente).
--
-- Estas tablas NO sustituyen a las de V2: el flujo de compartir un
-- recordatorio por correo sigue funcionando exactamente igual. Lo que se
-- añade es la vía familiar, que es por usuario y sirve para todo recurso.

-- ---------------------------------------------------------------------------
-- 1. Invitación a formar parte de una familia
-- ---------------------------------------------------------------------------
-- Por USUARIO, no por correo: se invita a alguien que ya se encontró
-- buscándolo por su nombre de usuario, así que no hay caso "invitado sin
-- cuenta" y no aplica el SEC-001 de no revelar si un correo existe.
--
-- Sin `expires_at`, a diferencia de `invitations` (V2): allí los 7 días son
-- una ASSUMPTION registrada para una invitación a un recordatorio concreto,
-- que caduca con él. Una invitación familiar no cuelga de ningún recurso y
-- no hay decisión de producto que fije una caducidad — inventarla sería
-- inventar un requisito. Se puede cancelar y se puede rechazar; eso basta.
CREATE TABLE family_invitations (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inviter_user_id  UUID NOT NULL REFERENCES users (id),
    invited_user_id  UUID NOT NULL REFERENCES users (id),
    status           VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at      TIMESTAMPTZ,
    CONSTRAINT ck_family_invitations_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED')),
    -- Nadie se invita a sí mismo.
    CONSTRAINT ck_family_invitations_not_self CHECK (inviter_user_id <> invited_user_id)
);

-- Respalda el 409 de invitación duplicada, igual que uq_invitations_pending_*
-- de V2: una sola PENDING por pareja y sentido.
CREATE UNIQUE INDEX uq_family_invitations_pending
    ON family_invitations (inviter_user_id, invited_user_id)
    WHERE status = 'PENDING';

CREATE INDEX ix_family_invitations_invited ON family_invitations (invited_user_id, status);
CREATE INDEX ix_family_invitations_inviter ON family_invitations (inviter_user_id, status);

-- ---------------------------------------------------------------------------
-- 2. Vínculo familiar aceptado
-- ---------------------------------------------------------------------------
-- ASSUMPTION EXPLÍCITA (no es una decisión del Product Owner): la familia se
-- modela como una relación SIMÉTRICA ENTRE DOS PERSONAS, no como un grupo con
-- identidad propia. Al aceptar se insertan las dos filas, así que "mi familia"
-- es un `WHERE user_id = ?` y nada más.
--
-- Por qué así y no un FAMILY_GROUP con miembros: un grupo obliga a decidir
-- cosas que nadie ha decidido — si A ya está en un grupo y B en otro, ¿aceptar
-- los fusiona?, ¿quién puede expulsar?, ¿queda alguien "dueño" del grupo?
-- Ninguna de esas preguntas aparece en el requisito, y responderlas por cuenta
-- propia sería inventar reglas de negocio. El modelo por parejas cubre todo lo
-- pedido (buscar, invitar, aceptar, listar, compartir) sin decidir nada de eso.
--
-- TBD: si más adelante se necesita un grupo con identidad, esta tabla se puede
-- derivar a él sin pérdida (cada pareja es una arista del grupo).
CREATE TABLE family_links (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users (id),
    relative_user_id  UUID NOT NULL REFERENCES users (id),
    invitation_id     UUID NOT NULL REFERENCES family_invitations (id),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_family_links_pair UNIQUE (user_id, relative_user_id),
    CONSTRAINT ck_family_links_not_self CHECK (user_id <> relative_user_id)
);

CREATE INDEX ix_family_links_user ON family_links (user_id);

-- ---------------------------------------------------------------------------
-- 3. Compartición de un recurso existente
-- ---------------------------------------------------------------------------
-- UNA tabla para los seis tipos de recurso, no una por módulo. El recurso NO
-- se duplica: aquí solo vive la RELACIÓN (quién comparte, con quién, sobre
-- qué, con qué responsabilidad y en qué estado). El registro original no se
-- toca ni gana columnas.
--
-- `resource_id` no lleva clave foránea a propósito: apunta a seis tablas
-- distintas según `resource_type`, y PostgreSQL no admite una FK polimórfica.
-- La integridad la garantiza el servicio, que carga y comprueba la propiedad
-- del recurso antes de insertar (misma vía que ya usa SharingService con
-- ReminderService.getOwnedOrThrow).
--
-- `responsibility`: compartir NO es siempre delegar. Solo es TRUE cuando el
-- recurso implica una acción y el dueño compromete a alguien con ella. Ver
-- ck_resource_shares_responsibility: hay tipos donde no tiene sentido y la
-- base de datos lo impide, no solo la interfaz.
CREATE TABLE resource_shares (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_type         VARCHAR(32) NOT NULL,
    resource_id           UUID NOT NULL,
    owner_user_id         UUID NOT NULL REFERENCES users (id),
    collaborator_user_id  UUID NOT NULL REFERENCES users (id),
    responsibility        BOOLEAN NOT NULL DEFAULT FALSE,
    part_done_at          TIMESTAMPTZ,
    status                VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    version               INTEGER NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_resource_shares_type
        CHECK (resource_type IN ('REMINDER', 'MAINTENANCE', 'SUBSCRIPTION',
                                 'WARRANTY', 'INVENTORY_ITEM', 'DOCUMENT')),
    CONSTRAINT ck_resource_shares_status CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT ck_resource_shares_not_self CHECK (owner_user_id <> collaborator_user_id),

    -- "No agregues estados o acciones que no tengan sentido para un recurso
    -- determinado" (requisito §5). Un artículo de inventario es algo que se
    -- POSEE, no algo que se hace: no hay "ya hice mi parte" de una silla. Un
    -- documento es explícitamente de solo visualización (requisito §6). En
    -- los dos casos compartir significa ver, y la regla vive aquí para que no
    -- dependa de que la interfaz se acuerde.
    CONSTRAINT ck_resource_shares_responsibility
        CHECK (responsibility = FALSE OR resource_type NOT IN ('INVENTORY_ITEM', 'DOCUMENT')),

    -- Marcar la parte hecha solo tiene sentido si hay parte que hacer.
    CONSTRAINT ck_resource_shares_part_done
        CHECK (part_done_at IS NULL OR responsibility = TRUE)
);

-- Un recurso se comparte una vez con cada persona; volver a compartirlo
-- después de revocar sí debe poder crear una fila nueva, de ahí el índice
-- parcial en vez de un UNIQUE a secas (mismo patrón que V2).
CREATE UNIQUE INDEX uq_resource_shares_active
    ON resource_shares (resource_type, resource_id, collaborator_user_id)
    WHERE status = 'ACTIVE';

-- Las dos consultas de la sección Compartidos: "me compartieron" y "yo compartí".
CREATE INDEX ix_resource_shares_collaborator ON resource_shares (collaborator_user_id, status);
CREATE INDEX ix_resource_shares_owner ON resource_shares (owner_user_id, status);
-- Y la de "¿con quién está compartido este recurso?", que usa cada ficha.
CREATE INDEX ix_resource_shares_resource ON resource_shares (resource_type, resource_id, status);
