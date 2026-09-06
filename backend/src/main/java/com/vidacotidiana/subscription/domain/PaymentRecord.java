package com.vidacotidiana.subscription.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * ADR-020(f): un ciclo pagado.
 *
 * Es lo único genuinamente nuevo que el rediseño añade al modelo, y existe
 * por una razón concreta: sin él, "marcar como pagado" no tendría dónde
 * anotarse y la lista mentiría al día siguiente — al avanzar la fecha al
 * siguiente ciclo se perdería el rastro de que el anterior se pagó.
 *
 * Además resuelve el importe variable: una tarjeta paga una cifra distinta
 * cada mes, y guardarla aquí evita pisar el importe estimado del pago.
 *
 * `periodDate` es la fecha del ciclo que se paga, NO el día en que se
 * marcó: así "pagué el de septiembre" y "pagué el de octubre" se
 * distinguen aunque ambos se registren el mismo día. El índice único sobre
 * (subscription_id, period_date) hace la operación idempotente.
 */
@Entity
@Table(name = "payment_records")
public class PaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "period_date", nullable = false)
    private LocalDate periodDate;

    @Column(name = "paid_on", nullable = false)
    private LocalDate paidOn;

    /** Importe REAL pagado. Puede diferir del estimado y puede ser nulo si
        el usuario solo quiere marcarlo como hecho. */
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaymentRecord() {
        // JPA
    }

    public PaymentRecord(UUID subscriptionId, UUID ownerUserId, LocalDate periodDate, LocalDate paidOn,
                         BigDecimal amount, String currency) {
        this.subscriptionId = subscriptionId;
        this.ownerUserId = ownerUserId;
        this.periodDate = periodDate;
        this.paidOn = paidOn;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public LocalDate getPeriodDate() {
        return periodDate;
    }

    public LocalDate getPaidOn() {
        return paidOn;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
