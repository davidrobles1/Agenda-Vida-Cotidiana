-- V40__push_due_log.sql
-- El registro de qué avisos de vencimiento ya se han mandado.
--
-- EL PROBLEMA: los avisos de pagos, garantías y mantenimientos solo se veían
-- con la aplicación abierta —los calculaba el cliente al cargar la pantalla—
-- así que un vencimiento con el teléfono en el bolsillo no avisaba de nada.
-- El push del backend solo cubría familia y compartición.
--
-- POR QUÉ HACE FALTA UNA TABLA: el trabajo que busca vencimientos se ejecuta
-- por reloj, y un reinicio del servidor, un despliegue o dos instancias a la
-- vez lo harían correr otra vez el mismo día. Sin dejar constancia, el
-- usuario recibiría el mismo aviso varias veces, que es la forma más rápida
-- de que desactive las notificaciones.
--
-- La clave primaria ES la regla: un aviso por recurso y día. Insertar dos
-- veces lo mismo falla, y esa colisión es justamente lo que evita el
-- duplicado, sin bloqueos ni coordinación entre instancias.

CREATE TABLE push_due_log (
    user_id       UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    resource_type VARCHAR(32) NOT NULL,
    resource_id   UUID        NOT NULL,
    notified_on   DATE        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, resource_type, resource_id, notified_on)
);

-- La limpieza de lo viejo se hace por fecha: es la única consulta que esta
-- tabla recibe además de la inserción.
CREATE INDEX idx_push_due_log_notified_on ON push_due_log (notified_on);
