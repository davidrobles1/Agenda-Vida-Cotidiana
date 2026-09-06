package com.vidacotidiana.subscription.domain;

import java.math.BigDecimal;

/**
 * ADR-020: los campos que la sección "Pagos" añadió a un compromiso.
 *
 * Viajan agrupados y no como catorce parámetros sueltos por una razón
 * práctica: el constructor y `applyEdit` de {@link Subscription} ya tenían
 * cinco argumentos, y sumarles catorce más produciría una firma imposible
 * de leer y trivial de equivocar (dos `BigDecimal` seguidos, tres
 * `Integer` seguidos). Agrupar no añade una capa: es el mismo dato, con
 * nombre.
 *
 * Todos los campos son opcionales. Un `null` en `applyPaymentDetails`
 * significa "no lo toques", igual que en `applyEdit`.
 */
public record PaymentDetails(
        PaymentKind kind,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String notes,
        Boolean variableAmount,
        // Solo CARD
        Integer statementDay,
        Integer dueDay,
        BigDecimal minPayment,
        BigDecimal noInterestPayment,
        String institution,
        String lastFour,
        // Solo CREDIT
        Integer totalInstallments,
        Integer currentInstallment
) {
    /** Lo que aplica a un alta que no manda ninguno de estos campos: el
        comportamiento exacto de antes de ADR-020. */
    public static PaymentDetails legacySubscription() {
        return new PaymentDetails(PaymentKind.SUBSCRIPTION, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
    }
}
