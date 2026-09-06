package com.vidacotidiana.subscription.domain;

/**
 * ADR-020(d): tipo de compromiso de pago.
 *
 * Seis valores, pero solo TRES formas estructurales — y esa distinción es
 * la que justifica que esto sea un tipo y no una categoría:
 *
 *   - Recurrente de importe fijo: SUBSCRIPTION, SERVICE, MEMBERSHIP,
 *     CUSTOM. Comparten campos exactamente (importe, periodicidad, próxima
 *     fecha); se diferencian solo en etiqueta e icono.
 *   - Tarjeta de crédito: CARD. Necesita campos que ninguna suscripción
 *     tiene —día de corte, día límite, importe variable—, y genera DOS
 *     fechas por ciclo en vez de una.
 *   - Crédito a plazos: CREDIT. Es la forma recurrente con final: cuenta
 *     "14 de 48" y deja de avisar al terminar.
 *
 * Una categoría es una etiqueta y no puede cambiar qué campos existen, que
 * es justo lo que separa a CARD del resto. Por eso esto es el tipo, y por
 * eso NO se añadió además una taxonomía de categorías (ADR-020(d)).
 */
public enum PaymentKind {
    SUBSCRIPTION,
    SERVICE,
    MEMBERSHIP,
    CUSTOM,
    CARD,
    CREDIT
}
