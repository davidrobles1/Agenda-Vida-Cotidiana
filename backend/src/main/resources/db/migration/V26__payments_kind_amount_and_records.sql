-- ADR-020: la sección "Suscripciones" pasa a "Pagos" — compromisos de pago
-- de cualquier tipo, con importe y divisa POR PAGO.
--
-- ADITIVA Y SIN PÉRDIDA: todas las columnas nuevas admiten NULL salvo
-- `kind`, que entra con DEFAULT 'SUBSCRIPTION'. Las suscripciones que ya
-- existen quedan exactamente como estaban —sin importe, sin divisa— y se
-- comportan igual que antes de esta migración.
--
-- La tabla conserva el nombre `subscriptions` (ADR-020(e)): renombrarla
-- obligaría a tocar entidad, repositorio, servicio, controlador, DTOs y
-- cliente para cero valor visible. El nombre de la sección es una etiqueta
-- de interfaz, no del almacenamiento.
--
-- ALCANCE DE LOS IMPORTES (ADR-020(b)): viven aquí y solo aquí. No se
-- añade `amount` a ninguna otra tabla, y esta migración no habilita
-- saldos, movimientos, estados de cuenta ni conexión bancaria.

-- ---------------------------------------------------------------
-- 1. Tipo de compromiso
-- ---------------------------------------------------------------
-- Seis valores, TRES formas estructurales (ADR-020(d)):
--   SUBSCRIPTION / SERVICE / MEMBERSHIP / CUSTOM -> recurrente de importe fijo
--   CARD                                          -> dos fechas por ciclo
--   CREDIT                                        -> recurrente con final
-- Sin CHECK a propósito, mismo criterio que `type` en
-- vision_board_elements: la validación vive en el DTO
-- (CreateSubscriptionRequest) y así añadir un tipo no exige migración.
ALTER TABLE subscriptions
    ADD COLUMN kind VARCHAR(24) NOT NULL DEFAULT 'SUBSCRIPTION';

-- ---------------------------------------------------------------
-- 2. Importe y divisa (comunes a todas las formas)
-- ---------------------------------------------------------------
-- NUMERIC(12,2) y no un flotante: un importe es dinero y no admite el
-- error de representación binaria. 12 dígitos cubren cualquier pago
-- doméstico sin quedarse corto en divisas de baja denominación.
ALTER TABLE subscriptions
    ADD COLUMN amount NUMERIC(12, 2);

-- ISO 4217 (MXN, USD, EUR...). Por pago, no por cuenta: decisión
-- explícita del Product Owner (ADR-020(c)) tomada ANTES de la primera
-- migración para no rehacer el modelo al aparecer el primer pago en otra
-- divisa.
ALTER TABLE subscriptions
    ADD COLUMN currency VARCHAR(3);

ALTER TABLE subscriptions
    ADD COLUMN payment_method VARCHAR(120);

ALTER TABLE subscriptions
    ADD COLUMN notes VARCHAR(2000);

-- ---------------------------------------------------------------
-- 3. Solo tarjeta de crédito (kind = 'CARD')
-- ---------------------------------------------------------------
-- Una tarjeta tiene DOS fechas por ciclo, no una: el corte (cuándo se sabe
-- cuánto debes) y el límite (cuándo hay que pagarlo). Se guardan como día
-- del mes (1-31) porque se repiten cada mes; `next_payment_date`, que ya
-- existía, sigue siendo la próxima fecha límite concreta.
ALTER TABLE subscriptions
    ADD COLUMN statement_day INTEGER;

ALTER TABLE subscriptions
    ADD COLUMN due_day INTEGER;

ALTER TABLE subscriptions
    ADD COLUMN min_payment NUMERIC(12, 2);

ALTER TABLE subscriptions
    ADD COLUMN no_interest_payment NUMERIC(12, 2);

ALTER TABLE subscriptions
    ADD COLUMN institution VARCHAR(120);

-- Etiqueta para reconocer la tarjeta, NO un dato bancario. Nunca se pide
-- ni se guarda el número completo, el CVV ni el titular.
ALTER TABLE subscriptions
    ADD COLUMN last_four VARCHAR(4);

-- El importe de una tarjeta es variable: se conoce al llegar el corte.
-- Esta marca permite mostrar "—" en vez de un $0 inventado, y vale también
-- para cualquier otro pago de importe cambiante (luz, agua).
ALTER TABLE subscriptions
    ADD COLUMN variable_amount BOOLEAN NOT NULL DEFAULT FALSE;

-- ---------------------------------------------------------------
-- 4. Solo crédito a plazos (kind = 'CREDIT')
-- ---------------------------------------------------------------
ALTER TABLE subscriptions
    ADD COLUMN total_installments INTEGER;

ALTER TABLE subscriptions
    ADD COLUMN current_installment INTEGER;

-- ---------------------------------------------------------------
-- 5. Historial de pagos realizados
-- ---------------------------------------------------------------
-- Lo único genuinamente nuevo del modelo (ADR-020(f)). Sin esto, "marcar
-- como pagado" no tendría dónde anotarse y la lista mentiría al día
-- siguiente; y el importe real de una tarjeta —que cambia cada mes— no
-- tendría dónde vivir sin pisar el importe estimado del pago.
CREATE TABLE payment_records (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscriptions (id) ON DELETE CASCADE,
    owner_user_id   UUID NOT NULL REFERENCES users (id),
    -- Fecha del ciclo que se está pagando: permite distinguir "pagué el de
    -- septiembre" de "pagué el de octubre" aunque ambos se registren el
    -- mismo día.
    period_date     DATE NOT NULL,
    -- Cuándo lo marcó el usuario.
    paid_on         DATE NOT NULL,
    -- Importe REAL pagado. Puede diferir del estimado (tarjetas, luz) y
    -- puede ser NULL si el usuario solo quiere marcarlo como hecho.
    amount          NUMERIC(12, 2),
    currency        VARCHAR(3),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_payment_records_subscription ON payment_records (subscription_id, period_date DESC);
CREATE INDEX ix_payment_records_owner ON payment_records (owner_user_id);

-- Un mismo ciclo no puede pagarse dos veces: es el equivalente en datos de
-- que el botón "marcar como pagado" sea idempotente.
CREATE UNIQUE INDEX ux_payment_records_period ON payment_records (subscription_id, period_date);

COMMENT ON COLUMN subscriptions.kind IS 'ADR-020: SUBSCRIPTION|SERVICE|MEMBERSHIP|CUSTOM|CARD|CREDIT.';
COMMENT ON COLUMN subscriptions.amount IS 'ADR-020: importe del pago. Alcance acotado a esta sección.';
COMMENT ON COLUMN subscriptions.currency IS 'ADR-020: ISO 4217, por pago (no por cuenta).';
COMMENT ON COLUMN subscriptions.last_four IS 'Etiqueta para reconocer la tarjeta. NO es un dato bancario.';
COMMENT ON TABLE payment_records IS 'ADR-020: pagos realizados. Un registro por ciclo pagado.';
