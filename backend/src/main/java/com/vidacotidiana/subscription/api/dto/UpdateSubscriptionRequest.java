package com.vidacotidiana.subscription.api.dto;

import com.vidacotidiana.subscription.domain.BillingCycle;

import java.time.Instant;

/** Todos los campos opcionales salvo version (edición parcial, mismo
    contrato que warranty.api.dto.UpdateWarrantyRequest). */
public record UpdateSubscriptionRequest(
        String service,
        String company,
        String plan,
        Instant nextPaymentDate,
        BillingCycle billingCycle,
        int version
) {
}
