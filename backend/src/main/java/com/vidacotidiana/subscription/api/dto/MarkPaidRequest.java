package com.vidacotidiana.subscription.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * ADR-020(f). Ambos campos opcionales: marcar como pagado sin escribir nada
 * es el caso normal (el importe ya está en el compromiso). Se envían solo
 * cuando el importe real difiere del estimado, que es lo habitual en una
 * tarjeta de crédito.
 */
public record MarkPaidRequest(
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @Pattern(regexp = "[A-Z]{3}") String currency
) {
}
