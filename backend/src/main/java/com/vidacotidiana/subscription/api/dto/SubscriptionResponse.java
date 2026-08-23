package com.vidacotidiana.subscription.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.subscription.domain.Subscription;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubscriptionResponse(
        UUID id,
        UUID ownerUserId,
        String service,
        String company,
        String plan,
        Instant nextPaymentDate,
        String billingCycle,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getOwnerUserId(),
                subscription.getService(),
                subscription.getCompany(),
                subscription.getPlan(),
                subscription.getNextPaymentDate(),
                subscription.getBillingCycle().name(),
                subscription.getVersion(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt()
        );
    }
}
