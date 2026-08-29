package com.vidacotidiana.subscription.api.dto;

import com.vidacotidiana.subscription.domain.BillingCycle;
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
        @Pattern(regexp = "PERSONAL|LABORAL") String context
) {
}
