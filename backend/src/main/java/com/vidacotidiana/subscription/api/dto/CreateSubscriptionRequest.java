package com.vidacotidiana.subscription.api.dto;

import com.vidacotidiana.subscription.domain.BillingCycle;
import java.math.BigDecimal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateSubscriptionRequest(
        @NotBlank @Size(min = 1, max = 200) String service,
        @Size(max = 200) String company,
        @Size(max = 200) String plan,
        @NotNull Instant nextPaymentDate,
        @NotNull BillingCycle billingCycle,
        /** ADR-019: módulo desde el que se crea. Ausente ⇒ PERSONAL. */
        @Pattern(regexp = "PERSONAL|LABORAL") String context,
        /** ADR-020. Ausente = SUBSCRIPTION, el comportamiento anterior. */
        @Pattern(regexp = "SUBSCRIPTION|SERVICE|MEMBERSHIP|CUSTOM|CARD|CREDIT") String kind,
        /** Importe del pago. Opcional: el alta no se bloquea por no saberlo,
            y una tarjeta no lo conoce hasta el corte. */
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        /** ISO 4217, por pago (ADR-020(c)). */
        @Pattern(regexp = "[A-Z]{3}") String currency,
        @Size(max = 120) String paymentMethod,
        @Size(max = 2000) String notes,
        Boolean variableAmount,
        @Min(1) @Max(31) Integer statementDay,
        @Min(1) @Max(31) Integer dueDay,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal minPayment,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal noInterestPayment,
        @Size(max = 120) String institution,
        /** Etiqueta para reconocer la tarjeta, NO un dato bancario. */
        @Pattern(regexp = "[0-9]{4}") String lastFour,
        @Min(1) @Max(600) Integer totalInstallments,
        @Min(0) @Max(600) Integer currentInstallment
) {

    /** Agrupa los campos de ADR-020 tal como los espera el dominio. */
    public com.vidacotidiana.subscription.domain.PaymentDetails toPaymentDetails() {
        return new com.vidacotidiana.subscription.domain.PaymentDetails(
                kind() == null ? null : com.vidacotidiana.subscription.domain.PaymentKind.valueOf(kind()),
                amount(), currency(), paymentMethod(), notes(), variableAmount(),
                statementDay(), dueDay(), minPayment(), noInterestPayment(), institution(), lastFour(),
                totalInstallments(), currentInstallment());
    }
}
