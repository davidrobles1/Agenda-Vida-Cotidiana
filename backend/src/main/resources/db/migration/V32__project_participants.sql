-- Participantes de un proyecto: quién más está en esta obra/caso, y con qué rol.
--
-- PROBLEMA QUE RESUELVE. Un proyecto solo sabía de UNA persona: su cliente
-- (`projects.client_person_id`, V11). Todos los demás —el proveedor, el
-- contacto en obra, el colaborador— no tenían dónde vivir, así que la única
-- forma de relacionarlos con el proyecto era colgarles una tarea o un
-- seguimiento y deducirlo de ahí. No se podía responder "¿quién está en esta
-- obra?".
--
-- POR QUÉ UNA TABLA Y NO UNA COLUMNA MÁS. Una persona participa en VARIOS
-- proyectos: el mismo proveedor trabaja en tres obras a la vez. Con columnas
-- habría que duplicar a la persona una vez por proyecto, y mantener tres
-- fichas del mismo proveedor sincronizadas a mano.
--
-- CONVIVENCIA CON `client_person_id` (DECISION del Product Owner,
-- 2026-09-06). El cliente NO se migra aquí: sigue siendo el campo del
-- proyecto. Para que no haya dos formas de decir lo mismo —y que acaben
-- discrepando— esta tabla cubre a los DEMÁS: CLIENTE no es un rol válido, y
-- el servicio rechaza añadir como participante a quien ya es el cliente.
-- Cliente y participantes son dos mitades disjuntas de "quién está aquí".
--
-- ON DELETE CASCADE en los dos lados, al revés que los enlaces de
-- garantía/mantenimiento con el inventario: una participación no significa
-- nada sin el proyecto ni sin la persona. Un comprobante de garantía sí
-- sobrevive al artículo —puede hacer falta para reclamar—, pero "Ana es
-- proveedora de una obra que ya no existe" no es un dato que conservar.
CREATE TABLE project_participants (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users (id),
    project_id    UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    person_id     UUID NOT NULL REFERENCES people (id) ON DELETE CASCADE,
    role          VARCHAR(32) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- La misma persona, una sola vez por proyecto. Respalda el 409 de
    -- duplicado en vez de dejar que dependa de una comprobación de lectura,
    -- que dos peticiones simultáneas pueden pasar a la vez.
    CONSTRAINT uq_project_participant UNIQUE (project_id, person_id),
    CONSTRAINT ck_project_participants_role
        CHECK (role IN ('PROVEEDOR', 'CONTACTO', 'COLABORADOR', 'OTRO'))
);

-- Las dos preguntas que se le hacen a esta tabla: "¿quién está en este
-- proyecto?" y "¿en qué proyectos está esta persona?".
CREATE INDEX ix_project_participants_project ON project_participants (project_id);
CREATE INDEX ix_project_participants_person ON project_participants (person_id);
CREATE INDEX ix_project_participants_owner ON project_participants (owner_user_id);

COMMENT ON TABLE project_participants IS
    'Personas que participan en un proyecto, con su rol. NO incluye al cliente: ese sigue siendo projects.client_person_id (DECISION 2026-09-06).';
COMMENT ON COLUMN project_participants.role IS
    'PROVEEDOR | CONTACTO | COLABORADOR | OTRO. CLIENTE queda deliberadamente fuera para no duplicar projects.client_person_id.';
