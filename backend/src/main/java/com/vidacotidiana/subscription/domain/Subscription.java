package com.vidacotidiana.subscription.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Módulo Suscripciones (pedido explícito del usuario, 2026-08-22) — mismo
 * shape/patrón que warranty.domain.Warranty (dueño, bloqueo optimista,
 * applyEdit parcial). Campos: service/company/plan/nextPaymentDate/
 * billingCycle — ver V15__subscriptions.sql para el porqué de cada uno
 * (en particular, por qué no hay campo de precio).
 */
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String service;

    @Column
    private String company;

    @Column
    private String plan;

    @Column(name = "next_payment_date", nullable = false)
    private Instant nextPaymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    private BillingCycle billingCycle;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Subscription() {
        // JPA
    }

    public Subscription(UUID ownerUserId, String service, String company, String plan, Instant nextPaymentDate, BillingCycle billingCycle) {
        this.ownerUserId = ownerUserId;
        this.service = service;
        this.company = company;
        this.plan = plan;
        this.nextPaymentDate = nextPaymentDate;
        this.billingCycle = billingCycle;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public String getService() {
        return service;
    }

    public String getCompany() {
        return company;
    }

    public String getPlan() {
        return plan;
    }

    public Instant getNextPaymentDate() {
        return nextPaymentDate;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isOwnedBy(UUID userId) {
        return this.ownerUserId.equals(userId);
    }

    public void applyEdit(String service, String company, String plan, Instant nextPaymentDate, BillingCycle billingCycle) {
        if (service != null) {
            this.service = service;
        }
        if (company != null) {
            this.company = company;
        }
        if (plan != null) {
            this.plan = plan;
        }
        if (nextPaymentDate != null) {
            this.nextPaymentDate = nextPaymentDate;
        }
        if (billingCycle != null) {
            this.billingCycle = billingCycle;
        }
        this.updatedAt = Instant.now();
    }
}
