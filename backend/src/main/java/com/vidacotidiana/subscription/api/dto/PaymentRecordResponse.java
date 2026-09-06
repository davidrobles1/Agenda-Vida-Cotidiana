package com.vidacotidiana.subscription.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.subscription.domain.PaymentRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** ADR-020(f): un ciclo pagado, tal como lo consume la pantalla de Pagos. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentRecordResponse(
        UUID id,
        UUID subscriptionId,
        LocalDate periodDate,
        LocalDate paidOn,
        BigDecimal amount,
        String currency
) {
    public static PaymentRecordResponse from(PaymentRecord record) {
        return new PaymentRecordResponse(
                record.getId(),
                record.getSubscriptionId(),
                record.getPeriodDate(),
                record.getPaidOn(),
                record.getAmount(),
                record.getCurrency());
    }
}
